package com.dq.mylibrary.Wifi

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.IBinder
import com.dq.mylibrary.Wifi.event.EndoBatteryEvents
import com.dq.mylibrary.Wifi.event.EndoSocketCloseEvents
import com.dq.mylibrary.Wifi.event.EndoSpeedEvents
import com.dq.mylibrary.Wifi.event.EndoConnectEvents
import com.dq.mylibrary.Wifi.event.EndoFinishAllEvents
import com.dq.mylibrary.dqLog
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * 心跳连接类  在基类中 启动
 */
class EndoSocketService : Service() {

    private var heart: Disposable? = null
    private var closeTime = 0 //关机指令计数
    var adressTime = 0 //获取地址为空的计数

    companion object {
        var lastTime = 0L //最后一次心跳包时间
        var timeOut = true //心跳超时

        var setWiFiSSID = false
        var setWiFiPWD = false

        var totalHistory = 0
        var battery = 80
        var speed = 0

        var isBatteryActivityShow = false //当前是否显示充电界面

        var hasPermissions = false
        var ijkPrepared = false

        var isEndoClose = false //冲牙器关机标志位
        var isEndoConnectShow = false //连接状态界面是否显示
        var lastConnectStatus = false //最后一次的连接状态

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
        startReceive()
        startHeart() //心跳机制
    }

    var oldAdress = ""
    private fun startHeart() {
        heart = Observable.interval(1000, TimeUnit.MILLISECONDS).subscribe {
            //收到关机指令时，拦截8次心跳
            if (isEndoClose) {
                timeOut = true
                closeTime++
                if (closeTime > 4) {
                    isEndoClose = false
                    closeTime = 0
                }
                return@subscribe
            }

            dqLog("当前地址为" + EndoSocketUtils.ADDRESS)
            if (EndoSocketUtils.ADDRESS.isEmpty()) {
                dqLog("发送 广播")
                sendBroadCast(this)
                adressTime++
                if (adressTime >= 5) {
                    postConnectEvents(false)
                    adressTime = 0
                } else {
                    EndoSocketUtils.ADDRESS = oldAdress
                }
            } else {
                oldAdress = EndoSocketUtils.ADDRESS
                adressTime = 0
                EndoCmdUtils.getInstance().sendCmd(EndoCmdSendType.HEART, "")

                if (lastTime != 0L){
                    timeOut = (System.currentTimeMillis() - lastTime) > 6 * 1000
                    dqLog("当前心跳是否超时$timeOut")
                    if (timeOut) {
                        EndoSocketUtils.ADDRESS = ""
                        postConnectEvents(false)
                    } else {
                        postConnectEvents(true)
                    }
                }
            }
        }
    }


    @Subscribe
    fun onRestartReceive(event: EndoSocketCloseEvents) {
        if (event.close) {
            startReceive()
        }
    }

    private fun postConnectEvents(connect: Boolean) {
        if (!connect) {
            ijkPrepared = false
        }
        dqLog("当前连接状态为$connect")
        EventBus.getDefault().post(EndoConnectEvents(connect))
    }

    @Subscribe
    fun onBatteryChange(event: EndoBatteryEvents) {
        /**
         * 优化app在后台时，自动弹出充电界面的问题
         * isBatteryActivityShow，判断充电界面是否显示
         */
//        if (event.reCharge && !isBatteryActivityShow && lastConnectStatus) {
//            startActivity(Intent(this, EndoBatteryActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            })
//        }
    }


    @Subscribe
    fun notifySpeed(event: EndoSpeedEvents) {
    }

    private fun startReceive() {
        thread {
            kotlin.run {
                EndoSocketUtils.server()
            }
        }
    }

    private fun sendBroadCast(context: Context) {
        thread {
            kotlin.run {
                EndoSocketUtils.sendBroadCastToCenter(context)
            }
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        heart?.dispose()
        super.onDestroy()
    }


    @SuppressLint("WifiManagerLeak")
    fun isWiFiConnect(): Boolean {
        (getSystemService(Activity.WIFI_SERVICE) as WifiManager).apply {
//            logi("isWiFiConnect isWiFiConnect $isWifiEnabled")
            if (!isWifiEnabled) {
                return false
            }
            connectionInfo?.let {
//                logi("isWiFiConnect supplicantState ${it.supplicantState}")
                return it.supplicantState == SupplicantState.COMPLETED
            }
        }
        return false
    }

    @Subscribe
    fun onFinishAll(event: EndoFinishAllEvents) {
        if (event.finish) {
            release()
            stopSelf()
        }
    }

    private fun release() {
        //lastTime = 0L
        timeOut = true

        setWiFiSSID = false
        setWiFiPWD = false

        totalHistory = 0
        battery = 80
        speed = 0

        isBatteryActivityShow = false

        hasPermissions = false
        ijkPrepared = false

        isEndoClose = false
        isEndoConnectShow = false
        lastConnectStatus = false
//        ConnectStatus = false
    }

}