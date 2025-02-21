package com.dq.mylibrary.ble

import android.bluetooth.BluetoothGatt
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException

interface BleListener {
    fun bleScanFail()

    fun onScanResult(scanResultList: MutableList<BleDevice>)

    fun onNotifyFailure(bleDevice: BleDevice)
    fun onCharacteristicChanged() // 接收到字节数据

    fun onConnectFail(bleDevice: BleDevice, exception: BleException)

    fun onDisConnected(isActiveDisConnected: Boolean,
                       device: BleDevice,
                       gatt: BluetoothGatt,
                       status: Int)

}