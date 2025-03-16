package com.tn.myapplication


import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.ToastUtils
import com.dq.mylibrary.base.BaseActivity
import com.dq.mylibrary.ble.BLE_EVENT_CONNECT_FAIL
import com.dq.mylibrary.ble.BLE_EVENT_DISCONNECT
import com.dq.mylibrary.ble.BLE_EVENT_NOTIFY_FAIL
import com.dq.mylibrary.ble.BLE_EVENT_NOTIFY_SUCCESS
import com.dq.mylibrary.ble.BLE_EVENT_SCAN_FAIL
import com.dq.mylibrary.ble.BLE_EVENT_SCAN_RESULT
import com.dq.mylibrary.ble.BleEvent

import com.dq.mylibrary.ble.BlePrepare
import com.dq.mylibrary.ble.FastBle.data.BleDevice
import com.dq.mylibrary.ble.FlowBle.BleScanner
import com.dq.mylibrary.dqLog
import com.tn.myapplication.databinding.ActivityBleBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow


/**
 * Created by T&N on 2022/03/04.
 * 1.Application 初始化
 * 2.权限申请
 * 3.扫描
 * 4.连接
 */
class BleActivity : BaseActivity<ActivityBleBinding>() {

    private lateinit var bleScanner: BleScanner

    override fun getViewId(): Int {
        return R.layout.activity_ble
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun initView() {
        super.initView()

        bleScanner = BleScanner(this)
    }

    private fun handleBleEvent(event: BleEvent) {
        dqLog("handleBleEvent: ${event.type}")
        when(event.type){
            BLE_EVENT_SCAN_FAIL -> {
                ToastUtils.showLong("BLE_EVENT_SCAN_FAIL")
            }
            BLE_EVENT_SCAN_RESULT->{
                var bleDevice = event.data as List<BleDevice>
                //直接连接
                dqLog("开始连接蓝牙")
                BlePrepare.instance.connectDescBle(bleDevice[0].mac)
            }
            BLE_EVENT_DISCONNECT->{
                ToastUtils.showLong("BLE_EVENT_DISCONNECT")
            }
            BLE_EVENT_CONNECT_FAIL->{
                ToastUtils.showLong("BLE_EVENT_CONNECT_FAIL")
            }
            BLE_EVENT_NOTIFY_FAIL->{
                ToastUtils.showLong("BLE_EVENT_NOTIFY_FAIL")
            }
            BLE_EVENT_NOTIFY_SUCCESS->{
                ToastUtils.showLong("BLE_EVENT_NOTIFY_SUCCESS")
            }
        }
    }


    /**
     * 设备去重与缓存
     */
    fun scascanDevices():Flow<BluetoothDevice> = flow {

        //创建 设备缓存集合
        val deviceCache = mutableSetOf<String>()

        bleScanner.startScan().collect{ state->
            when (state) {
                is BleScanner.ScanState.FoundDevice -> {
                    val address = state.device.address
                    // 当前缓存没有
                    if (!deviceCache.contains(address)) {
                        deviceCache.add(address)
                        // 将设备发送到 Flow中
                        emit(state.device)
                    }
                }
                else -> Unit
            }
        }
    }.buffer(50 )//添加背压处理

//    private fun handleScanState(scanState: BleScanner.ScanState) {
//        when (scanState) {
//            BleScanner.ScanState.Started -> {
//                dqLog("扫描开始")
//                ToastUtils.showLong("扫描开始")
//            }
//            BleScanner.ScanState.Stopped -> {
//                dqLog("扫描停止")
//                ToastUtils.showLong("扫描停止")
//            }
//            is BleScanner.ScanState.FoundDevice -> {
////                dqLog("发现设备: ${scanState.device.name}, RSSI: ${scanState.rssi}")
////                ToastUtils.showLong("发现设备: ${scanState.device.name}, RSSI: ${scanState.rssi}")
//                // 连接设备
//                connectToDevice(scanState.device)
//            }
//        }
//    }
}