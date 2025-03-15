package com.dq.mylibrary.Wifi.cmd

import com.dq.mylibrary.Wifi.EndoSocketUtils
import com.dq.mylibrary.printStack
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread
import kotlin.experimental.and

abstract class EndoBaseCmd {

    private val sendPort = 8019

    abstract fun initCmd(data: String): ByteArray
    abstract fun getData(cmd: ByteArray): String?

    fun getCommon(bytes: ByteArray) {
        bytes[0] = 0x55.toByte()
        bytes[1] = 0x66.toByte()
        bytes[2] = 0x11.toByte()
        bytes[3] = 0x01.toByte()
    }

    fun sendCmd(data: String) {
        GlobalScope.launch {
            var theSocket: DatagramSocket? = null
            try {
                val server = InetAddress.getByName(EndoSocketUtils.ADDRESS)
                theSocket = DatagramSocket()
                val cmd = initCmd(data)
//                SolexLogUtil.dq_log("发送数据包" + EndoSocketUtils.bytesToHexString(cmd) + " IP 段端口为" + EndoSocketUtils.ADDRESS)
                val theOutput = DatagramPacket(cmd, cmd.size, server, sendPort)
                theSocket.send(theOutput)
            } catch (e: IOException) {
//                logi(" receive bytesToHexString   异常 ")
//                SolexLogUtil.dq_log("发送数据包异常 " + e.message)
                printStack(e)
            } finally {
                theSocket?.close()
            }
        }
    }

    fun sendCmdFirst(data: String) {
        thread {
            kotlin.run {
                var theSocket: DatagramSocket? = null
                try {
                    val server = InetAddress.getByName(EndoSocketUtils.ADDRESS)
                    theSocket = DatagramSocket()
                    val cmd = initCmd(data)
                    cmd[2] = 0x10.toByte()
//                    logi(" receive bytesToHexString " + EndoSocketUtils.bytesToHexString(cmd))
                    val theOutput = DatagramPacket(cmd, cmd.size, server, sendPort)
                    theSocket.send(theOutput)
                } catch (e: IOException) {
                    printStack(e)
                } finally {
                    theSocket?.close()
                }
            }
        }
    }

    @Suppress("unused")
    fun byteToHex(b: Byte): String? {
        return Integer.toHexString(b.and(0xFF.toByte()).toInt())
    }

}