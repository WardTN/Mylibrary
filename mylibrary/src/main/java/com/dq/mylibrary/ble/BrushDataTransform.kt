package com.dq.mylibrary.ble

import com.clj.fastble.data.BleDevice
import com.clj.fastble.utils.HexUtil
import com.dq.mylibrary.dqLog
import java.nio.charset.StandardCharsets




fun inttohex(count: Int): String? {
    return Integer.toHexString(count)
}

fun byteArrayToString(byteArr: ByteArray?): String? {
    return String(byteArr!!, StandardCharsets.UTF_8)
}

var dataPress: ByteArray? = null

/**
 * 解析收到的蓝牙数据
 */
fun parseNotifyData(data: ByteArray, bleDevice: BleDevice) {
    val datstr = HexUtil.encodeHexStr(data)
    dqLog("数据内容:$datstr")

}



private fun byteToInt(b1: Byte, b2: Byte): Int {
    return b1.toInt() and 0xFF shl 8 or (b2.toInt() and 0xFF)
}

private fun hextode(str: String): Int {
    return Integer.valueOf(str, 16)
}

/**
 * 将int转为16进制string
 */
private fun intArrayToHexStringArray(arrayList: MutableList<Int>): String {
    val stringBuilder = StringBuilder()
    for (i in 0 until arrayList.size) {
        val hexString = Integer.toHexString(arrayList[i])
        stringBuilder.append(if (hexString.length == 1) "0$hexString" else hexString)
    }
    return stringBuilder.toString()
}


/**
 * 将字符串编程转16进制
 */
private fun stringTo0xIntArray(data: String): IntArray {
    val dataArray = data.toCharArray()
    val dataInt = IntArray(dataArray.size / 2)
    for (i in dataArray.indices step 2) {
        dataInt[i / 2] = Integer.parseInt("" + dataArray[i] + dataArray[i + 1], 16)
    }
    return dataInt
}

fun byteToInt(b1: Byte): Int {
    return b1.toInt() and 0xFF
}

