package com.tn.myapplication


import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.clj.fastble.data.BleDevice
import com.dq.mylibrary.base.BaseActivity
import com.dq.mylibrary.ble.BLE_EVENT_CONNECT_FAIL
import com.dq.mylibrary.ble.BLE_EVENT_DISCONNECT
import com.dq.mylibrary.ble.BLE_EVENT_NOTIFY_FAIL
import com.dq.mylibrary.ble.BLE_EVENT_NOTIFY_SUCCESS
import com.dq.mylibrary.ble.BLE_EVENT_SCAN_FAIL
import com.dq.mylibrary.ble.BLE_EVENT_SCAN_RESULT
import com.dq.mylibrary.ble.BleEvent

import com.dq.mylibrary.ble.BlePrepare
import com.dq.mylibrary.dqLog
import com.tn.myapplication.databinding.ActivityBleBinding
import kotlinx.coroutines.launch


/**
 * Created by T&N on 2022/03/04.
 * 1.Application 初始化
 * 2.权限申请
 * 3.扫描
 * 4.连接
 */
class BleActivity : BaseActivity<ActivityBleBinding>() {
    override fun getViewId(): Int {
        return R.layout.activity_ble
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun initView() {
        super.initView()
        BlePrepare.instance.checkPermission(this)
        var bleMsgManager = BleMsgManager()
        databinding.btnScan.setOnClickListener {
            BlePrepare.instance.startScanBle(bleMsgManager)
        }

        // 启动协程来收集 bleEvents
        lifecycleScope.launch {
            bleMsgManager.bleEvents.collect { event ->
                handleBleEvent(event)
            }
        }
    }

    //断开设备
//    BleManager.getInstance().disconnectAllDevice()



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


}