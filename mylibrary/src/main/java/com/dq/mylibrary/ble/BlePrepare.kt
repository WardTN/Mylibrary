package com.dq.mylibrary.ble

import android.app.Application
import android.bluetooth.BluetoothGatt
import android.os.Handler
import android.os.HandlerThread
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig
import com.dq.mylibrary.dqLog

class BlePrepare private constructor() {

    private var reConnCount = 0 // 重连次数
    private var scanFailCount = 0 // 扫描失败次数
    private val workThread = HandlerThread("BlePrepare").apply { start() }
    private val workHandler = Handler(workThread.looper)

    var curBleDev: BleDevice? = null
    private var bleDevBean: BleDevBean? = null
    private var bleMsgManager: BaseBleManager? = null

    companion object {
        val instance: BlePrepare by lazy { BlePrepare() }
        private const val MAX_CONNECT_COUNT = 3
    }

    fun initBle(application: Application, bleDevBean: BleDevBean) {
        this.bleDevBean = bleDevBean
        setupBleManager(application)
        setupScanRule(bleDevBean)
    }

    private fun setupBleManager(application: Application) {
        BleManager.getInstance().apply {
            init(application)
            enableLog(true)
            setReConnectCount(1, 3000)
            operateTimeout = 10_000
        }
    }

    private fun setupScanRule(bleDevBean: BleDevBean) {
        val scanRuleConfig = BleScanRuleConfig.Builder()
            .setDeviceName(true, bleDevBean.wifiName)
            .setScanTimeOut(bleDevBean.scanTimeOut)
            .build()
        BleManager.getInstance().initScanRule(scanRuleConfig)
    }

    fun startScanBle(bleMsgManager: BaseBleManager){
        this.bleMsgManager = bleMsgManager
        startScanBle()
    }


    fun startScanBle() {
        bleDevBean?.let {
            if (scanFailCount >= it.maxScanCount) {
                scanFailCount = 0
                bleMsgManager?.bleScanFail()
            } else {
                workHandler.post { BleManager.getInstance().scan(scanCallback) }
            }
        }
    }

    private val scanCallback = object : BleScanCallback() {
        override fun onScanStarted(success: Boolean) {}

        override fun onScanning(bleDevice: BleDevice?) {}

        override fun onScanFinished(scanResultList: MutableList<BleDevice>?) {
            dqLog("BlePrepare_扫描结束")
            bleDevBean?.let {
                if (scanResultList.isNullOrEmpty() || !isExistDescBle(scanResultList, it.wifiName)) {
                    retryScan()
                } else {
                    scanFailCount = 0
                    bleMsgManager?.onScanResult(scanResultList)
                }
            }
        }
    }

    private fun retryScan() {
        scanFailCount++
        startScanBle()
    }

    private fun isExistDescBle(list: List<BleDevice>?, wifiName: String): Boolean {
        return list?.any { it.name?.contains(wifiName) == true } == true
    }

    fun connectDescBle(list: MutableList<BleDevice>, bleName: String) {
        val targetBle = list.firstOrNull { it.name?.contains(bleName) == true }
        targetBle?.let { startConnectBle(it.mac) }
    }

    fun connectDescBle(mac: String) {
        startConnectBle(mac)
    }

    private fun startConnectBle(mac: String?) {
        mac?.let { workHandler.post { connectBle(it) } }
    }

    fun disConnectBle() {
        curBleDev?.let {
            BleManager.getInstance().disconnect(it)
        }
        curBleDev = null
    }

    private fun connectBle(mac: String) {
        BleManager.getInstance().connect(mac, object : BleGattCallback() {
            override fun onStartConnect() {}

            override fun onDisConnected(
                isActiveDisConnected: Boolean,
                device: BleDevice,
                gatt: BluetoothGatt,
                status: Int
            ) {
                dqLog("BlePrepare_蓝牙断开连接")
                curBleDev = null
                bleMsgManager?.onDisConnected(isActiveDisConnected, device, gatt, status)
            }

            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
            }

            override fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                dqLog("BlePrepare_连接成功")
                curBleDev = bleDevice
                bleMsgManager?.clearCommandCache()
                bleMsgManager?.bleNotify(curBleDev, gatt)

            }

            override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                if (++reConnCount < MAX_CONNECT_COUNT) {
                    connectBle(mac)
                } else {
                    reConnCount = 0
                    bleMsgManager?.onConnectFail(bleDevice, exception)
                    dqLog("BlePrepare_连接失败")
                }
            }
        })
    }
}
