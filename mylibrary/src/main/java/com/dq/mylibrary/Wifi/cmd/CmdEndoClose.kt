package com.dq.mylibrary.Wifi.cmd

import com.dq.mylibrary.Wifi.EndoSocketService
import com.solex.endo.midea.util.EndoSocketUtils
import com.solex.endo.midea.util.SolexLogUtil


/**
 * 断开连接 指令处理
 */
class CmdEndoClose : EndoBaseCmd() {

    override fun initCmd(data: String): ByteArray {
        val bytes = ByteArray(16)
        getCommon(bytes)
        bytes[4] = 0x08.toByte()
        bytes[5] = 0x00.toByte()
        bytes[6] = 0x01.toByte()

        return bytes
    }

    override fun getData(cmd: ByteArray): String? {
        EndoSocketService.lastConnectStatus = false
        EndoSocketService.hasDeviceCloseCmd = true
        EndoSocketService.heartTimeOut = true
        EndoSocketService.lastHeartCmdTime = 0
        EndoSocketUtils.ADDRESS = ""
        SolexLogUtil.dq_log("测肤笔 接收到断开连接指令")
        return null
    }

}