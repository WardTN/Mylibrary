package com.dq.mylibrary.ble

import android.bluetooth.BluetoothGatt
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleWriteCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import java.util.*

abstract class BaseBleManager:BleListener {

    protected val TAG = BaseBleManager::class.java.simpleName

    // 缓存请求指令，避免指令乱序发送
    private val saveCommandList: Queue<ByteArray> = LinkedList()

    // 判断当前请求是否空闲
    private var isFree: Boolean = true

    // 工作线程
    protected val workHandler: Handler

    protected lateinit var bleDevice: BleDevice

//    // 间隔发送心跳
//    protected val heartbeatInterval: Observable<Long>
//    protected var heartbeatSubscribe: Disposable? = null

    // 每条指令请求次数
    val PER_REQUEST_COUNT = 3


    init {
        val handlerThread = HandlerThread("bleContract")
        handlerThread.start()
        workHandler = Handler(handlerThread.looper)
//        heartbeatInterval = Observable.interval(0, getHeartbeatDelay(), TimeUnit.MILLISECONDS)
    }

//    /**
//     * 发送心跳
//     */
//    fun sendHeartbeat() {
//        heartbeatSubscribe = heartbeatInterval.subscribe {
//            if (!isDeviceConnected()) {
//                stopHeartbeat()
//                return@subscribe
//            }
//            val querydata = getHeartbeatCommand()
//            workHandler.post { sendCommand(querydata) }
//        }
//    }

//    /**
//     * 停止心跳
//     */
//    fun stopHeartbeat() {
//        heartbeatSubscribe?.dispose()
//        heartbeatSubscribe = null
//    }


    /**
     * 发送蓝牙消息
     */
    fun sendBleMsg() {
        if (!isDeviceConnected()) return
    }


    /**
     * 清除缓存指令
     */
    fun clearCommandCache() {
        saveCommandList.clear()
        isFree = true
    }

    /**
     * 发送请求 排队进行
     */
    protected fun sendCommand(commandList: ByteArray, reqCount: Int = 3) {
        if (!isDeviceConnected()) return
        if (commandList.isEmpty()) return
        if (isFree) {
            isFree = false
            startSend(commandList, reqCount)
        } else {
            if (saveCommandList.size >= 5) saveCommandList.remove()
            saveCommandList.add(commandList)
        }
    }

    private fun startSend(commandList: ByteArray, reqCount: Int = 3) {
        sendByteArrayToBle(commandList, reqCount)
    }

    /**
     * 发送指令到设备
     */
    private fun sendByteArrayToBle(dataString: ByteArray, reqCount: Int) {
        if (!BleManager.getInstance().isBlueEnable) return
        BleManager.getInstance().write(
            BlePrepare.instance.curBleDev!!,
            getServiceUUID(),
            getWriteUUID(),
            dataString,
            object : BleWriteCallback() {
                override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray?) {
                    // 一条指令发送全部完成
                    if (PER_REQUEST_COUNT == reqCount) {
                        // 判断是否还有任务
                        judgeTask()
                    }
                }

                override fun onWriteFailure(exception: BleException?) {
                    Log.e(TAG, "指令发送失败！---${exception.toString()}")
                    if (PER_REQUEST_COUNT == reqCount) {
                        // 判断是否还有任务
                        judgeTask()
                    }
                }
            })
    }


    /**
     * 判断是否有缓存的请求指令
     */
    private fun judgeTask() {
        if (saveCommandList.isEmpty()) {
            isFree = true
            return
        }
        workHandler.postDelayed({
            if (saveCommandList.isNotEmpty()) {
                startSend(saveCommandList.poll())
            }
        }, 200)
    }


    /**
     * 接收蓝牙通知
     */
    fun bleNotify(device: BleDevice?, bluetoothGatt: BluetoothGatt?) {
        bleDevice = device!!
        workHandler.post {
            BleManager.getInstance().notify(
                bleDevice,
                getServiceUUID(),
                getNotifyUUID(),
                bleNotifyCallback
            )
        }
    }


    // 蓝牙数据监听
    private val bleNotifyCallback = object : BleNotifyCallback() {
        override fun onCharacteristicChanged(data: ByteArray?) {
            if (data == null) {
                Log.d(TAG, "接收: null")
                return
            }
            Log.d(TAG, "接收: 数据长度--${data.size}")
            workHandler.post { parseNotifyData(data, bleDevice) }
        }

        override fun onNotifyFailure(exception: BleException?) {
            Log.e(TAG, "onNotifyFailure")
            onNotifyFailureEvent(exception)
        }

        override fun onNotifySuccess() {
            Log.e(TAG, "onNotifySuccess")
            onNotifySuccessEvent()
        }
    }

    /**
     * 获取服务UUID
     */
    protected abstract fun getServiceUUID(): String

    /**
     * 获取写UUID
     */
    protected abstract fun getWriteUUID(): String

    /**
     * 获取通知UUID
     */
    protected abstract fun getNotifyUUID(): String


    /**
     * 解析通知数据
     */
    protected abstract fun parseNotifyData(data: ByteArray, device: BleDevice)

    /**
     * 处理通知失败事件
     */
    protected abstract fun onNotifyFailureEvent(exception: BleException?)

    /**
     * 处理通知成功事件
     */
    protected abstract fun onNotifySuccessEvent()

    /**
     * 检查设备是否连接
     */
    protected fun isDeviceConnected(): Boolean {
        return BlePrepare.instance.curBleDev != null && BleManager.getInstance()
            .isConnected(BlePrepare.instance.curBleDev)
    }

    /**
     * 打印日志
     */
    protected fun print(type: String, data: String) {
        Log.d(TAG, "$type: $data")
    }

    abstract fun bleDisConnect()
}