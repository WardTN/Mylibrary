package com.dq.mylibrary.ble.FlowBle

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID

/**
 * 特征值 读写扩展
 */


/**
 * 使用指定的特征写入数据到蓝牙设备.
 * 此函数通过协程挂起，直到数据写入成功或失败.
 *
 * @param characteristic 要写入数据的蓝牙特征.
 * @param data 要写入的字节数组.
 * @return 返回一个布尔值，表示数据是否成功写入.
 */
suspend fun BluetoothGatt.safeWriteCharacteristic(
    characteristic: BluetoothGattCharacteristic, data: ByteArray
): Boolean = suspendCancellableCoroutine { continuation ->
    // 设置特征的值为待写入的数据.
    characteristic.value = data
    // 调用系统接口写入特征.
    writeCharacteristic(characteristic)

    // 创建一个匿名的BluetoothGattCallback来监听写入操作的结果.
    val callback = object : BluetoothGattCallback() {
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
        ) {
            // 当特征写入完成时，恢复协程并传递写入是否成功的布尔值.
            continuation.resume(status == BluetoothGatt.GATT_SUCCESS) {}
        }
    }

    // 当协程被取消时，关闭资源.
    continuation.invokeOnCancellation {
        close()
    }
}


/**
 * 启用蓝牙设备的特征值通知功能，并返回一个Flow以接收通知数据
 *
 * 此函数使用Kotlin的Flow构建，以异步方式处理蓝牙设备的特征值变化通知
 * 它首先启用指定特征值的通知，然后配置客户端特性配置描述符（CCCD），以接收特征值的变化
 * 当特征值发生变化时，它会通过Flow发送新的特征值数据
 *
 * @param characteristic 蓝牙设备的特征值，用于启用通知
 * @return 返回一个Flow，用于接收特征值的通知数据
 */
// TODO: 这边要增加 notify 是否成功回调函数
fun BluetoothGatt.enableNotifications(characteristic: BluetoothGattCharacteristic): Flow<ByteArray> =
    callbackFlow {
        // CCCD的UUID，用于配置特征值的通知功能
        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // 启用通知
        setCharacteristicNotification(characteristic, true)

        // 配置CCCD
        characteristic.getDescriptor(cccdUuid)?.let { descriptor ->
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            writeDescriptor(descriptor)
        }

        // 监听数据变化
        val callback = object : BluetoothGattCallback() {
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
            ) {
                trySend(characteristic.value)
            }
        }

        // 等待Flow的关闭，并在关闭时禁用通知
        awaitClose {
            setCharacteristicNotification(characteristic, false)
            close()
        }
    }

// 权限检查扩展函数
fun Activity.checkBlePermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PERMISSION_GRANTED
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
        } else {
            TODO("VERSION.SDK_INT < M")
        }
    }
}

// 权限请求封装
@RequiresApi(Build.VERSION_CODES.M)
fun Activity.requestBlePermissions(requestCode: Int) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    requestPermissions(permissions, requestCode)
}


// 特征值读取
fun readCharacteristic(characteristic: BluetoothGattCharacteristic){
//    if (!checkReadPermission(characteristic)) return
//    enqueueCommand(BleCommand.Read(characteristic).apply {
//        onExecute = { gatt ->
//            if (!gatt.readCharacteristic(characteristic)) {
//                completeCommand()
//            }
//        }
//    })

}

//
//sealed class BleCommand {
//    abstract fun execute(gatt: BluetoothGatt)
//
//    data class Read(val characteristic: BluetoothGattCharacteristic) : BleCommand()
//    data class Write(val characteristic: BluetoothGattCharacteristic) : BleCommand()
//    // 其他命令类型...
//}









