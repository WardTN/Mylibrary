package com.dq.mylibrary.ble.FlowBle

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch


/**
 * Ble 扫描核心实现
 */

class BleScanner(private val context: Context) {
    /**
     * 蓝牙适配器 第一次访问时才被初始化
     */
    private val bluetoochAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private var scanner: BluetoothLeScanner? = null

    //标识 正在运行的协程任务
    private var scanJob: Job? = null


    //使用密封类封装扫面结果
    sealed class ScanState {
        object Started : ScanState()
        object Stopped : ScanState()
        data class FoundDevice(val device: BluetoothDevice, val rssi: Int) : ScanState()
    }

    //启动扫描 （带超时控制）
    fun startScan(
        filters: List<ScanFilter> = emptyList(),
        settings: ScanSettings = defaultScanSettings(),
        timeouMill: Long = 10_000
    ): Flow<ScanState> = callbackFlow {

        // 获取 BluetoochLeScanner 实例
        scanner = bluetoochAdapter?.bluetoothLeScanner ?: return@callbackFlow

        // 创建ScanCallBack 实例, 用于处理扫面结果
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // 发送 FoundDevice 状态到Flow
                trySend(ScanState.FoundDevice(result.device, result.rssi))
            }
        }

        // 发送 Started 状态到 Flow
        trySend(ScanState.Started)

        // 启动蓝牙扫描
        scanner?.startScan(filters, settings, callback)

        // 自动超时停止
        scanJob = launch {
            delay(timeouMill)
            stopScan(callback)
        }

        awaitClose { stopScan(callback) }
    }

    private fun stopScan(callback: ScanCallback) {
        scanner?.stopScan(callback)
        scanJob?.cancel()
        scanner = null
    }


    /**
     * 设置默认的扫面配置
     * 该函数使用ScanSettings.Builder来创建并配置扫描设置对象它将扫描模式设置为低延迟模式，
     * @return 配置好的ScanSettings对象，用于蓝牙设备的扫描
     */
    private fun defaultScanSettings() =
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

}