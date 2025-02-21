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
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit


//class BrushEventContractUtils private constructor() {
//
//    private val TAG = BrushEventContractUtils::class.java.simpleName
//
//    object SingleHolder {
//        val obj = BrushEventContractUtils()
//    }
//
//    companion object {
//        val mInstance: BrushEventContractUtils
//            get() = SingleHolder.obj
//    }
//
//
//    // 缓存请求指令，避免指令乱序发送
//    private var saveCommandList = LinkedList<ByteArray>()
//
//    // 判断当前请求是否空闲
//    private var isFree: Boolean = true
//
//    //工作线程
//    private val workHandler: Handler
//
//    private lateinit var bleDevice: BleDevice
//
//    //间隔发送心跳
//    private val heartbeatInterval: Observable<Long>
//    private var heartbeatSubscribe: Disposable? = null
//
//    init {
//        val handlerThread = HandlerThread("bleContract")
//        handlerThread.start()
//        workHandler = Handler(handlerThread.looper)
//        heartbeatInterval =
//            Observable.interval(0, HEARTBEAT_DELAY, TimeUnit.MILLISECONDS)
//    }
//
//    /**
//     * 发送心跳
//     */
//    fun sendHeartbeat() {
//        heartbeatSubscribe = heartbeatInterval.subscribe {
//            if (null == BlePrepare.instance.curBleDev ||
//                !BleManager.getInstance().isConnected(BlePrepare.instance.curBleDev)
//            ) {
//                stopHeartbeat()
//                return@subscribe
//            }
////            val heartData =
////                WaterBedBleDataTransform.editSendBleData(mutableListOf(COMMAND_SEND_HEARTBEAT))
////               sendToBle(heartData, 1)
//            val querydata = CmdUtil.sendHeart(0x01)
//            workHandler.post {
//                sendCommand(querydata)
//            }
//        }
////        sendQueryDryCommand(null)
//    }
//
//    /**
//     * 停止心跳
//     */
//    fun stopHeartbeat() {
//        heartbeatSubscribe?.dispose()
//        heartbeatSubscribe = null
//    }
//
////    /**
////     * 震动
////     */
////    fun sendVibrateCommand(typeCode: Byte) {
////        if (null == BlePrepare.instance.curBleDev || !BleManager.getInstance()
////                .isConnected(BlePrepare.instance.curBleDev)
////        ) {
////            return
////        }
////        val querydata = CmdUtil.sendVibrateData(typeCode);
////        workHandler.post {
////            sendCommand(querydata)
////        }
////    }
////
////    fun sendCalibrationValueCommand(value: ByteArray) {
//////        if (null == BlePrepare.instance.curBleDev || !BleManager.getInstance()
//////                .isConnected(BlePrepare.instance.curBleDev)
//////        ) {
//////            return
//////        }
////        val arrdata = byteArrayOf(0.toByte())
////        val querydata = CmdUtil.sendCalibrationValueData(arrdata, value, 0x05);
////        workHandler.post {
////            sendCommand(querydata)
////        }
////    }
////
////    fun sendSetEms(level: Byte, status: Byte) {
////        if (null == BlePrepare.instance.curBleDev || !BleManager.getInstance()
////                .isConnected(BlePrepare.instance.curBleDev)
////        ) {
////            return
////        }
////        val querydata = CmdUtil.sendEms(0x04, level, status)
////        workHandler.post {
////            sendCommand(querydata)
////        }
////    }
////
////    fun sendSetMassage(way: Byte, level: Byte, status: Byte) {
////        if (null == BlePrepare.instance.curBleDev || !BleManager.getInstance()
////                .isConnected(BlePrepare.instance.curBleDev)
////        ) {
////            return
////        }
////        val querydata = CmdUtil.sendMassage(0x05, way, level, status)
////        workHandler.post {
////            sendCommand(querydata)
////        }
////    }
//
//
//
//
//
//    /**
//     * 清除缓存指令
//     */
//    fun clearCommandCache() {
//        saveCommandList.clear()
//        isFree = true
//    }
//
//    /**
//     * 发送请求 排队进行
//     */
//    private fun sendCommand(commandList: ByteArray, reqCount: Int = 3) {
//        if (null == BlePrepare.instance.curBleDev || !BleManager.getInstance()
//                .isConnected(BlePrepare.instance.curBleDev)
//        ) {
//            //  stopHeartbeat()
//            return
//        }
//        if (commandList.isEmpty()) {
//            return
//        }
//        // 请求空闲-->直接请求
//        if (isFree) {
//            isFree = false
//            startSend(commandList, reqCount)
//            return
//        }
//        if (saveCommandList.size >= 5) {
//            saveCommandList.removeAt(0)
//        }
//        // 请求阻塞-->则缓存到集合中
//        saveCommandList.add(commandList)
//    }
//
//    private fun startSend(commandList: ByteArray, reqCount: Int = 3) {
////        val dataInts = AdjustBleDataTransform.editSendBleData(commandList)
//        sendByteArrayToBle(commandList, reqCount)
//    }
//
////    /**
////     * 发送指令到设备
////     */
////    private fun sendToBle(dataString: String, reqCount: Int) {
////        if (!BleManager.getInstance().isBlueEnable) {
////            return
////        }
////        print("发送", dataString)
////        BleManager.getInstance().write(BlePrepare.instance.curBleDev,
////                UUID_SERVICE,
////                UUID_WRITE,
////                HexUtil.hexStringToBytes(dataString),
////                object : BleWriteCallback() {
////                    override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray?) {
////                        // 一条指令发送全部完成
////                        if (PER_REQUEST_COUNT == reqCount) {
////                            // 判断是否还有任务
////                            judgeTask()
////                        }
////                    }
////
////                    override fun onWriteFailure(exception: BleException?) {
////                        println("指令发送失败！---${exception.toString()}")
////                        // 一条指令发送全部完成
////                        if (PER_REQUEST_COUNT == reqCount) {
////                            // 判断是否还有任务
////                            judgeTask()
////                        }
////                    }
////                })
////    }
//
//
//    /**
//     * 发送指令到设备
//     */
//    private fun sendByteArrayToBle(dataString: ByteArray, reqCount: Int) {
//        if (!BleManager.getInstance().isBlueEnable) {
//            return
//        }
//
//        BleManager.getInstance().write(
//            BlePrepare.instance.curBleDev,
//            UUID_SERVICE,
//            UUID_WRITE,
//            dataString,
//            object : BleWriteCallback() {
//                override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray?) {
//                    // 一条指令发送全部完成
//                    if (PER_REQUEST_COUNT == reqCount) {
//                        // 判断是否还有任务
//                        judgeTask()
//                    }
//                }
//
//                override fun onWriteFailure(exception: BleException?) {
//                    println("指令发送失败！---${exception.toString()}")
//                    // 一条指令发送全部完成
//                    if (PER_REQUEST_COUNT == reqCount) {
//                        // 判断是否还有任务
//                        judgeTask()
//                    }
//                }
//            })
//    }
//
//    /**
//     * 判断是否有缓存的请求指令
//     */
//    private fun judgeTask() {
//        // 无缓存指令-->请求空闲状态
//        if (saveCommandList.isEmpty()) {
//            isFree = true
//            return
//        }
//        workHandler.postDelayed({
//            if (saveCommandList.size > 0) {
//                startSend(saveCommandList[0])
//                saveCommandList.removeAt(0)
//            }
//        }, 200)
//    }
//
//    /**
//     * 接收蓝牙通知
//     */
//    fun bleNotify(device: BleDevice?, bluetoothGatt: BluetoothGatt?) {
//        bleDevice = device!!
//        workHandler.post {
//            BleManager.getInstance().notify(
//                bleDevice,
//                UUID_SERVICE,
//                UUID_NOTIFY,
//                bleNotifyCallback
//            )
//        }
//    }
//
//    //蓝牙数据监听
//    private val bleNotifyCallback = object : BleNotifyCallback() {
//        override fun onCharacteristicChanged(data: ByteArray?) {
//            if (null == data) {
//                print("接收", "null")
//                return
//            }
//            print("接收", "数据长度--${data.size}")
//            workHandler.post {
//                parseNotifyData(data, bleDevice)
//            }
//        }
//
//        override fun onNotifyFailure(exception: BleException?) {
//            Log.e("CHEN","onNotifyFailure")
//            EventBus.getDefault().post(BleConnectResultEvent(bleDevice,  false))
//        }
//
//        override fun onNotifySuccess() {
//            //开始发送心跳
////            mInstance.sendHeartbeat()
//            EventBus.getDefault().post(BleDisConnectEvent(false, bleDevice.mac, bleDevice.name))
////            EventBus.getDefault().post(BleConnectResultEvent(bleDevice,  true))
//            Log.e("CHEN","onNotifySuccess")
//        }
//    }
//
//    private fun print(type: String, data: String) {
//        println("$TAG----$type: $data")
//    }
//}