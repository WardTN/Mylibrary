package com.dq.mylibrary.ble.zhBle

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.text.TextUtils
import com.dq.mylibrary.dqLog
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import java.util.UUID


/**
 * Bluetooth Low Energy (BLE) 客户端类，用于管理BLE设备的连接、断开连接、数据读写等操作。
 * 该类实现了Client接口，提供了一系列蓝牙操作的API。
 *
 * @param context Android上下文，用于访问应用程序资源。
 * @param bluetoothAdapter 蓝牙适配器，用于扫描和连接BLE设备。
 * @param serviceUUID BLE服务的UUID，用于指定连接的服务。
 * @param logTag 日志标记，用于在日志中标识特定的信息。
 */
@SuppressLint("MissingPermission")
class BleClient(
    override val context: Context,
    override val bluetoothAdapter: BluetoothAdapter?,
    override var serviceUUID: UUID?,
    override val logTag: String
) : Client {


    // 根据Android版本设置写入类型
    override var writeType: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
    } else {
        -1
    }

    // 扫描设备回调，用于处理设备扫描结果
    private lateinit var scanDeviceCallback: ScanDeviceCallback
    // 连接状态回调，用于处理连接状态变化
    private lateinit var connectionStateCallback: ConnectionStateCallback
    // 接收数据的映射，键为特征UUID，值为接收数据的回调函数
    private val receiveDataMap = HashMap<UUID, (ByteArray) -> Unit>()
    // 发送数据的映射，键为数据，值为发送结果的回调函数
    private val writeDataMap = HashMap<ByteArray, DataResultCallback>()
    // 读取数据的映射，键为特征UUID，值为读取结果的回调函数
    private val readDataMap = HashMap<UUID, DataResultCallback>()
    // BluetoothGatt对象，用于与BLE设备进行交互
    private var bluetoothGatt: BluetoothGatt? = null
    // MTU值，用于设置最大传输单元
    private var mtu = 0
    // 定时器，用于管理超时任务
    private val timer = Timer()
    // 当前的超时任务
    private var timeoutTask: TimerTask? = null


    // 扫描回调，用于处理设备扫描结果
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (TextUtils.isEmpty(device?.name)) {
                return
            }
            scanDeviceCallback.call(Device(device, false))
        }
    }


    /**
     * 开始扫描BLE设备。
     * @param callback 扫描结果回调，当发现设备时调用。
     */
    override fun startScan(callback: ScanDeviceCallback) {
        scanDeviceCallback = callback
        bluetoothAdapter!!.bluetoothLeScanner!!.startScan(
            null,
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
            scanCallback
        )
    }

    /**
     * 停止扫描BLE设备。
     */
    override fun stopScan() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        }
    }

    // BluetoothGatt回调，用于处理与BLE设备的交互
    private val gattCallback = object : BluetoothGattCallback() {
        private var discoveredServices = false
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                discoveredServices = false
                var mtuResult = false
                if (mtu > 0) {
                    mtuResult = changeMtu(mtu)
                }
                if (!mtuResult) {
                    discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            dqLog("$logTag --> onServicesDiscovered: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                discoveredServices = true
                if (serviceUUID == null) {
                    callConnectionState(ConnectionState.CONNECTED)
                    return
                }
                val gattService = bluetoothGatt!!.getService(serviceUUID)
                if (gattService != null) {
                    callConnectionState(ConnectionState.CONNECTED)
                } else {
//                    dqLog("$logTag --> onServicesDiscovered: getService($serviceUUID)=null")
                    callConnectionState(ConnectionState.CONNECT_ERROR)
                }
            } else {
                callConnectionState(ConnectionState.CONNECT_ERROR)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION") receiveDataMap[characteristic.uuid]?.invoke(characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray
        ) {
            receiveDataMap[characteristic.uuid]?.invoke(value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            @Suppress("DEPRECATION") val value = characteristic.value
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                dqLog("$logTag --> onCharacteristicWrite: value=${String(value)}, status=$status")
                callSendDataResult(value, status == BluetoothGatt.GATT_SUCCESS)
            } else {
                //api33及以上获取不到value
                dqLog("$logTag --> onCharacteristicWrite: value=${value ?: null}, status=$status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            @Suppress("DEPRECATION") val value = characteristic.value
            dqLog("$logTag --> onCharacteristicRead: value=${value.contentToString()}, status=$status")
            callReadDataResult(characteristic.uuid, status == BluetoothGatt.GATT_SUCCESS, value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            dqLog("$logTag --> onCharacteristicRead2: value=${value.contentToString()}, status=$status")
            callReadDataResult(characteristic.uuid, status == BluetoothGatt.GATT_SUCCESS, value)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            dqLog("$logTag --> onMtuChanged: status=${status}, mtu=$mtu")
            if (!discoveredServices) {
                discoverServices()
            }
        }
    }


    /**
     * 发现BLE设备的服务。
     */
    private fun discoverServices() {
        if (!bluetoothGatt!!.discoverServices()) {
//            dqLog("$logTag --> discoverServices=false")
            callConnectionState(ConnectionState.CONNECT_ERROR)
        }
    }


    /**
     * 连接BLE设备。
     * @param device 要连接的设备。
     * @param mtu MTU值，用于设置最大传输单元。
     * @param timeoutMillis 超时时间，单位为毫秒。
     * @param stateCallback 连接状态回调，用于处理连接状态变化。
     */
    override fun connect(
        device: Device, mtu: Int, timeoutMillis: Long, stateCallback: ConnectionStateCallback
    ) {
        this.mtu = mtu
        connectionStateCallback = stateCallback
        connectionStateCallback.call(ConnectionState.CONNECTING)
        scheduleTimeoutTask(timeoutMillis) {
            callConnectionState(ConnectionState.CONNECT_TIMEOUT)
        }
        val realDevice = bluetoothAdapter!!.getRemoteDevice(device.address)
        bluetoothGatt?.safeClose()
        bluetoothGatt = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                realDevice.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE,
                    BluetoothDevice.PHY_LE_1M_MASK
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                realDevice.connectGatt(
                    context, false, gattCallback, BluetoothDevice.TRANSPORT_LE
                )
            }

            else -> {
                realDevice.connectGatt(context, false, gattCallback)
            }
        }
    }

    /**
     * 调用连接状态回调。
     * @param state 连接状态。
     */
    private fun callConnectionState(state: ConnectionState) {
        cancelTimeoutTask()
        if (state == ConnectionState.CONNECT_TIMEOUT || state == ConnectionState.CONNECT_ERROR) {
            bluetoothGatt?.safeClose()
        }
        connectionStateCallback.call(state)
    }

    /**
     * 改变MTU值。
     * @param mtu 新的MTU值。
     * @return 如果请求改变MTU成功，则返回true；否则返回false。
     */
    override fun changeMtu(mtu: Int): Boolean {
        this.mtu = mtu
        val mtuResult = bluetoothGatt?.requestMtu(this.mtu) == true
        dqLog("$logTag --> requestMtu($mtu)=$mtuResult")
        return mtuResult
    }

    /**
     * 获取支持的服务列表。
     * @return 支持的服务列表，如果为空则返回null。
     */
    override fun supportedServices(): List<Service>? {
        return bluetoothGatt?.services?.map { getGattService ->
            Service(getGattService.uuid,
                Service.typeOf(getGattService.type),
                getGattService.characteristics.map {
                    Characteristic(
                        it.uuid, Characteristic.getProperties(it.properties), it.permissions
                    )
                },
                getGattService.includedServices.map { includedService ->
                    Service(
                        includedService.uuid,
                        Service.typeOf(includedService.type),
                        includedService.characteristics.map {
                            Characteristic(
                                it.uuid, Characteristic.getProperties(it.properties), it.permissions
                            )
                        },
                        null
                    )
                })
        }
    }

    /**
     * 分配服务UUID。
     * @param service 要分配的服务。
     */
    override fun assignService(service: Service) {
        serviceUUID = service.uuid
    }

    /**
     * 获取Gatt服务。
     * @return 如果获取成功，则返回BluetoothGattService对象；否则返回null。
     */
    private fun getGattService(): BluetoothGattService? {
        if (bluetoothGatt == null) {
            dqLog("$logTag --> 设备未连接!")
            return null
        }
        if (serviceUUID == null) {
            dqLog("$logTag --> 未设置serviceUUID!")
            return null
        }
        val gattService = bluetoothGatt!!.getService(serviceUUID)
        if (gattService == null) {
            dqLog("$logTag --> getService($serviceUUID)=null!")
            return null
        }
        return gattService
    }

    /**
     * 接收数据。
     * @param uuid 特征的UUID。
     * @param onReceive 接收到数据时的回调函数。
     * @return 如果设置接收数据成功，则返回true；否则返回false。
     */
    override fun receiveData(uuid: UUID?, onReceive: (ByteArray) -> Unit): Boolean {
        val gattService = getGattService() ?: return false
        receiveDataMap[uuid!!] = onReceive
        val receiveCharacteristic = gattService.getCharacteristic(uuid)
        if (receiveCharacteristic == null) {
            dqLog("$logTag --> receiveData: getCharacteristic($uuid)=null")
            return false
        }
        return setCharacteristicNotification(receiveCharacteristic, true)
    }

    /**
     * 取消接收数据。
     * @param uuid 特征的UUID。
     * @return 如果取消接收数据成功，则返回true；否则返回false。
     */
    override fun cancelReceive(uuid: UUID?): Boolean {
        val gattService = getGattService() ?: return false
        receiveDataMap.remove(uuid)
        val receiveCharacteristic = gattService.getCharacteristic(uuid)
        if (receiveCharacteristic == null) {
            dqLog("$logTag --> cancelReceive: getCharacteristic($uuid)=null")
            return false
        }
        return setCharacteristicNotification(receiveCharacteristic, false)
    }

    /**
     * 设置特征值通知
     * 此函数用于启用或禁用指定特征值的通知
     *
     * @param receiveCharacteristic 要设置通知的特征值
     * @param enable                是否启用通知
     * @return                      设置通知是否成功
     */
    private fun setCharacteristicNotification(
        receiveCharacteristic: BluetoothGattCharacteristic, enable: Boolean
    ): Boolean {
        // 设置特征值通知
        val notificationResult =
            bluetoothGatt!!.setCharacteristicNotification(receiveCharacteristic, enable)
        dqLog("$logTag --> setCharacteristicNotification($enable), result=$notificationResult")

        // 根据启用状态设置描述符值
        val descriptorValue = if (enable) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }

        // 遍历特征值的描述符并设置其值
        @Suppress("DEPRECATION") receiveCharacteristic.descriptors.forEach {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                it.value = descriptorValue
                bluetoothGatt!!.writeDescriptor(it)
            } else {
                bluetoothGatt!!.writeDescriptor(it, descriptorValue)
            }
        }

        return notificationResult
    }

    /**
     * 发送数据
     * 此函数用于向指定特征值发送数据
     *
     * @param uuid          特征值的UUID
     * @param data          要发送的数据
     * @param timeoutMillis 超时时间
     * @param callback      发送结果回调
     */
    override fun sendData(
        uuid: UUID?, data: ByteArray, timeoutMillis: Long, callback: DataResultCallback
    ) {
        // 获取服务
        val gattService = getGattService()
        if (gattService == null) {
            callback.call(false, data)
            return
        }

        // 将回调存储到映射中
        writeDataMap[data] = callback

        // 设置超时任务
        scheduleTimeoutTask(timeoutMillis) {
            dqLog("$logTag --> sendData timeout")
            callSendDataResult(data, false)
        }

        // 获取写入特征值
        val writeCharacteristic = gattService.getCharacteristic(uuid)
        if (writeCharacteristic != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION") writeCharacteristic.value = data
                if (writeType != -1) {
                    writeCharacteristic.writeType = writeType
                }
                @Suppress("DEPRECATION") if (!bluetoothGatt!!.writeCharacteristic(
                        writeCharacteristic
                    )
                ) {
                    dqLog("$logTag --> sendData: writeCharacteristic=false")
                    callSendDataResult(data, false)
                }
            } else {
                val result =
                    bluetoothGatt!!.writeCharacteristic(writeCharacteristic, data, writeType)
                dqLog("$logTag --> sendData: writeCharacteristic=$result")
                if (result == BluetoothStatusCodes.SUCCESS) {
                    callSendDataResult(data, true)
                } else {
                    callSendDataResult(data, false)
                }
            }
        } else {
            dqLog("$logTag --> sendData: getCharacteristic($uuid)=null")
            callSendDataResult(data, false)
        }
    }

    /**
     * 调用发送数据结果回调
     *
     * @param data     发送的数据
     * @param success  发送是否成功
     */
    private fun callSendDataResult(data: ByteArray, success: Boolean) {
        cancelTimeoutTask()
        writeDataMap.remove(data)?.call(success, data)
    }

    /**
     * 读取数据
     * 此函数用于读取指定特征值的数据
     *
     * @param uuid          特征值的UUID
     * @param timeoutMillis 超时时间
     * @param callback      读取结果回调
     */
    override fun readData(uuid: UUID?, timeoutMillis: Long, callback: DataResultCallback) {
        // 获取服务
        val gattService = getGattService()
        if (gattService == null) {
            callback.call(false, null)
            return
        }

        // 将回调存储到映射中
        readDataMap[uuid!!] = callback

        // 设置超时任务
        scheduleTimeoutTask(timeoutMillis) {
//            dqLog("$logTag --> readData timeout")
            callReadDataResult(uuid, false)
        }

        // 获取读取特征值
        val readCharacteristic = gattService.getCharacteristic(uuid)
        if (readCharacteristic != null) {
            if (!bluetoothGatt!!.readCharacteristic(readCharacteristic)) {
//                dqLog("$logTag --> readData: readCharacteristic=false")
                callReadDataResult(uuid, false)
            }
        } else {
//            dqLog("$logTag --> readData: getCharacteristic($uuid)=null")
            callReadDataResult(uuid, false)
        }
    }

    /**
     * 调用读取数据结果回调
     *
     * @param uuid    读取数据的特征值UUID
     * @param success 读取是否成功
     * @param data    读取到的数据
     */
    private fun callReadDataResult(uuid: UUID, success: Boolean, data: ByteArray? = null) {
        cancelTimeoutTask()
        readDataMap.remove(uuid)?.call(success, data)
    }

    /**
     * 安排超时任务
     *
     * @param timeoutMillis 超时时间
     * @param onTask        超时后的任务
     */
    private fun scheduleTimeoutTask(timeoutMillis: Long, onTask: () -> Unit) {
        cancelTimeoutTask()
        timeoutTask = object : TimerTask() {
            override fun run() {
                onTask()
            }
        }
        timer.schedule(timeoutTask, timeoutMillis)
    }

    /**
     * 取消超时任务
     */
    private fun cancelTimeoutTask() {
        timeoutTask?.cancel()
        timeoutTask = null
    }

    /**
     * 断开连接
     * 此函数用于断开与设备的连接并清理资源
     */
    override fun disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt?.safeClose()
            bluetoothGatt = null
            receiveDataMap.clear()
            writeDataMap.clear()
            callConnectionState(ConnectionState.DISCONNECTED)
        }
    }

    /**
     * 安全关闭BluetoothGatt
     */
    private fun BluetoothGatt.safeClose() = try {
        disconnect()
        close()
    } catch (_: IOException) {

    }

    /**
     * 释放资源
     * 此函数用于释放所有资源，包括断开连接和取消定时器
     */
    override fun release() {
        disconnect()
        timer.cancel()
    }

}