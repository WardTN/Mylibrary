package com.dq.mylibrary.ble.FlowBle

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService

/**
 * 密封类，用于表示连接状态
 * 密封类可以有子类，但所有可能的子类都必须在与密封类相同的文件中声明
 * 这使得密封类非常适合用于表示受限的类型集，例如当一个值只能是几种特定状态之一时
 */
sealed class ConnectionState {

    object Disconnected : ConnectionState()

    data class Connecting(val device:BluetoothDevice):ConnectionState()
    data class Connected(val gatt: BluetoothGatt) : ConnectionState()
    data class ServicesDiscovered(val services: List<BluetoothGattService>) : ConnectionState()
    data class Error(val message: String,val device: BluetoothDevice) : ConnectionState()
}