package com.dq.mylibrary.ble.FlowBle

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * BleConnector 类用于管理与蓝牙设备的连接状态
 * 它提供了设备连接和断开连接的功能，并使用协程和通道来处理连接状态的变化
 *
 * @param context 应用程序上下文，用于访问蓝牙适配器
 * @param scope 协程作用域，用于启动协程任务
 */
class BleConnector(
    private val context: Context,  val scope: CoroutineScope
) {

    // BluetoothGatt 对象用于与蓝牙设备进行通信
    private var bluetoochGatt: BluetoothGatt? = null

    // 使用 Channel 管理连接事件，允许在协程之间发送和接收连接状态的变化
    private val connectionEventChannel = Channel<ConnectionState>()

    // 将 Channel 转换为 Flow，以便在协程中更容易地处理连接事件
    val connectionEvents = connectionEventChannel.receiveAsFlow()

    /**
     * 使用指定的蓝牙设备在限定时间内尝试建立连接
     *
     * 此函数尝试在指定的超时时间内与蓝牙设备建立连接如果连接或断开连接
     * 超过了指定时间，则会抛出 TimeoutCancellationException 异常，在 catch 块中
     * 处理此异常，通过 connectionEventChannel 发送连接错误信息，并关闭连接
     *
     * @param device 要连接的蓝牙设备
     * @param timeout 连接超时时间，单位为毫秒
     */
    private suspend fun connectWithTime(device: BluetoothDevice, timeout: Long) {
        try {
            // 在指定的超时时间内尝试执行连接操作
            withTimeout(timeout) {
                // 尝试连接到指定的蓝牙设备
                connect(device)
                // 等待连接完成，直到设备连接成功或发生错误
                connectionEvents.first {
                    it is ConnectionState.Connected || it is ConnectionState.Error
                }
            }
        } catch (e: TimeoutCancellationException) {
            // 如果连接超时，发送错误信息并通过 connectionEventChannel
            connectionEventChannel.send(ConnectionState.Error("连接超时",device))
            // 关闭连接
            close()
        }
    }


    /**
     * 设备连接入口
     * 启动与指定蓝牙设备的连接过程
     *
     * @param device 要连接的蓝牙设备
     */
    fun connect(device: BluetoothDevice) = scope.launch {
        connectionEventChannel.send(ConnectionState.Connecting(device))

        bluetoochGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt, status: Int, newState: Int
            ) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectionEventChannel.trySend(ConnectionState.Connected(gatt))
                        gatt.discoverServices()
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectionEventChannel.trySend(ConnectionState.Error("连接断开",device))
                        close()
                    }
                }
            }
        })
    }

    fun close() {
        bluetoochGatt?.disconnect()
        bluetoochGatt?.close()
        connectionEventChannel.close()
    }

    fun closeGattResource(){
        bluetoochGatt?.run {
            disconnect()
            close()
            bluetoochGatt = null
        }
    }

//    private fun retryWithBluetoothReset() {
//        if (BluetoothAdapter.getDefaultAdapter().disable()){
//            handler
//        }
//
//    }
}