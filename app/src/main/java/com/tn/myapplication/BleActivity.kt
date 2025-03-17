package com.tn.myapplication


import android.bluetooth.BluetoothDevice
import android.os.Build
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
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
import com.dq.mylibrary.ble.zhBle.ClientType
import com.dq.mylibrary.ble.zhBle.CoroutineClient
import com.dq.mylibrary.dqLog
import com.tn.myapplication.databinding.ActivityBleBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.UUID


/**
 * Created by T&N on 2022/03/04.
 * 1.Application 初始化
 * 2.权限申请
 * 3.扫描
 * 4.连接
 */
class BleActivity : BaseActivity<ActivityBleBinding>() {

//    private lateinit var bleScanner: BleScanner

    private val serviceUid: UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d1910")
    private val receiveUid: UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b10")
    private val sendUid: UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b10")

    private lateinit var bluetoothClient: CoroutineClient
    private var bluetoothType by mutableStateOf(ClientType.BLE)


    override fun getViewId(): Int {
        return R.layout.activity_ble
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun initView() {
        super.initView()

        /*   bleScanner = BleScanner(this)

           findViewById<Button>(R.id.btn_scan).setOnClickListener {
               //启动协程 来 收集Flow 数据
               lifecycleScope.launch {
                   scascanDevices().collect{ device->
   //                    dqLog("扫描到设备：${device.name}")
   //                    ToastUtils.showShort("扫描到设备：${device.name}")
                   }
               }
           }*/

    }

    /**
     * 扫描蓝牙设备，并返回一个Flow以发布扫描到的设备信息
     * 此函数旨在避免重复扫描相同的设备，通过维护一个设备缓存集合来过滤重复的设备信息
     * 使用缓冲策略来处理可能的背压问题，确保在设备快速扫描到大量设备时能够有效处理
     *
     * @return Flow<BluetoothDevice> 返回一个Flow，用于发布扫描到的蓝牙设备对象
     */
//    fun scascanDevices():Flow<BluetoothDevice> = flow {
//
//        // 创建一个用于缓存设备地址的集合，以避免重复发送相同的设备信息
//        val deviceCache = mutableSetOf<String>()
//
//        // 启动蓝牙低功耗设备扫描
//        bleScanner.startScan().collect{ state->
//            when (state) {
//                is BleScanner.ScanState.FoundDevice -> {
//                    // 获取扫描到的设备地址
//                    val address = state.device.address
//                    // 检查当前设备是否已经在缓存中
//                    if (!deviceCache.contains(address)) {
//                        // 如果不在缓存中，则添加到缓存
//                        deviceCache.add(address)
//                        // 并将设备发送到Flow中，供下游处理
//                        emit(state.device)
//                    }
//                }
//                else -> Unit // 其他扫描状态不做处理
//            }
//        }
//    }.buffer(50) // 添加背压处理，缓冲最多50个设备信息

    // 1.扫描
    private fun startScanDevice() {
        bluetoothClient = CoroutineClient(this@BleActivity, bluetoothType, serviceUid)
        //创建线程实现扫描
        lifecycleScope.launch {
            bluetoothClient.startScan(SCAN_TIME_OUT).collect { device ->
                dqLog("扫描到设备：${device.name}")
            }
        }
    }
}