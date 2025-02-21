package com.dq.mylibrary.ble

import android.bluetooth.BluetoothGatt
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException

class BleMsgManager : BaseBleManager(){

    //FEASYCOM
    val UUID_SERVICE = "0000FFF0-0000-1000-8000-00805F9B34FB"
    val UUID_WRITE = "0000FFF2-0000-1000-8000-00805F9B34FB"
    val UUID_NOTIFY = "0000FFF1-0000-1000-8000-00805F9B34FB"


    override fun getServiceUUID(): String {
        return UUID_SERVICE
    }

    override fun getWriteUUID(): String {
        return UUID_WRITE
    }

    override fun getNotifyUUID(): String {
        return UUID_NOTIFY
    }

    override fun parseNotifyData(data: ByteArray, device: BleDevice) {

    }

    override fun onNotifyFailureEvent(exception: BleException?) {

    }

    override fun onNotifySuccessEvent() {

    }

    override fun bleDisConnect() {

    }

    override fun bleScanFail() {

    }

    override fun onScanResult(scanResultList: MutableList<BleDevice>) {
        TODO("Not yet implemented")
    }

    override fun onNotifyFailure(bleDevice: BleDevice) {
        TODO("Not yet implemented")
    }

    override fun onCharacteristicChanged() {
        TODO("Not yet implemented")
    }

    override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
        TODO("Not yet implemented")
    }

    override fun onDisConnected(
        isActiveDisConnected: Boolean,
        device: BleDevice,
        gatt: BluetoothGatt,
        status: Int,
    ) {
        TODO("Not yet implemented")
    }
}