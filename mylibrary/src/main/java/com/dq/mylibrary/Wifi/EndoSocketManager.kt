package com.dq.mylibrary.tcp

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.SupplicantState
import android.os.SystemClock
import androidx.annotation.SuppressLint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

class EndoSocketManager(private val context: Context){

    private var heartJob: Job? = null
    private var closeTime = 0 // 关机指令计数
    private var adressTime = 0 // 获取地址为空的计数

    companion object {
        var lastTime = 0L // 最后一次心跳包时间
        var timeOut = true // 心跳超时

        var setWiFiSSID = false
        var setWiFiPWD = false

        var totalHistory = 0
        var battery = 80
        var speed = 0

        var isBatteryActivityShow = false // 当前是否显示充电界面

        var hasPermissions = false
        var ijkPrepared = false

        var isEndoClose = false // 冲牙器关机标志位
        var isEndoConnectShow = false // 连接状态界面是否显示
        var lastConnectStatus = false // 最后一次的连接状态
    }

    private val eventFlow = MutableSharedFlow<EndoEvent>(replay = 0, extraBufferCapacity = Channel.UNLIMITED)

    fun start() {
        startReceive()
        startHeart() // 心跳机制
    }

    fun stop() {
        heartJob?.cancel()
        release()
    }

    private fun startHeart() {
        heartJob = GlobalScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(1000) // 每秒执行一次

                // 收到关机指令时，拦截8次心跳
                if (isEndoClose) {
                    timeOut = true
                    closeTime++
                    if (closeTime > 4) {
                        isEndoClose = false
                        closeTime = 0
                    }
                    continue
                }

                dqLog("当前地址为${EndoSocketUtils.ADDRESS}")
                if (EndoSocketUtils.ADDRESS.isEmpty()) {
                    dqLog("发送 广播")
                    sendBroadCast(context)
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

                    if (lastTime != 0L) {
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
    }

    private fun postConnectEvents(connect: Boolean) {
        if (!connect) {
            ijkPrepared = false
        }
        dqLog("当前连接状态为$connect")
        eventFlow.emit(EndoEvent.ConnectEvent(connect))
    }

    private fun startReceive() {
        GlobalScope.launch(Dispatchers.IO) {
            EndoSocketUtils.server()
        }
    }

    private fun sendBroadCast(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            EndoSocketUtils.sendBroadCastToCenter(context)
        }
    }

    fun getEventFlow(): SharedFlow<EndoEvent> {
        return eventFlow
    }

    @SuppressLint("WifiManagerLeak")
    fun isWiFiConnect(): Boolean {
        (context.getSystemService(Context.WIFI_SERVICE) as WifiManager).apply {
            if (!isWifiEnabled) {
                return false
            }
            connectionInfo?.let {
                return it.supplicantState == SupplicantState.COMPLETED
            }
        }
        return false
    }

    private fun release() {
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
    }

    sealed class EndoEvent {
        data class ConnectEvent(val isConnected: Boolean) : EndoEvent()
        // 可以添加其他事件类型
    }

    private var oldAdress = ""

    private fun dqLog(message: String) {
        // 实现日志记录逻辑
        println(message)
    }
}