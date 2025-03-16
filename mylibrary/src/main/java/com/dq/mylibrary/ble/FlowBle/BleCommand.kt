package com.dq.mylibrary.ble.FlowBle

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean

private val commandQueue = LinkedList<BleCommand>()
private var isExecuting = AtomicBoolean(false)

sealed class BleCommand {
//    abstract fun execute(gatt: BluetoothGatt)

    data class Read(val characteristic: BluetoothGattCharacteristic) : BleCommand()
    data class Write(val characteristic: BluetoothGattCharacteristic) : BleCommand()
    // 其他命令类型...
}

//private fun enqueueCommand(command: BleCommand) {
//    synchronized(commandQueue) {
//        commandQueue.offer(command)
//        if (!isExecuting.get()) processNextCommand()
//    }
//}
//
//private fun processNextCommand() {
//    CoroutineScope(Dispatchers.IO).launch {
//        val cmd = synchronized(commandQueue) { commandQueue.poll() }
//        cmd?.let {
//            isExecuting.set(true)
//            it.execute(bluetoothGatt)
//        }
//    }
//}
//
//fun completeCommand() {
//    isExecuting.set(false)
//    processNextCommand()
//}

