package com.dq.mylibrary.Wifi.cmd

import com.solex.endo.midea.ui.service.EndoSocketService
import com.solex.endo.midea.util.*


/**
 * 广播 CMD
 */
class CmdEndoBroadCast : EndoBaseCmd() {

    override fun initCmd(data: String): ByteArray {
        var bytes = ByteArray(16)
        getCommon(bytes)
        bytes[2] = 0x10.toByte()
        bytes[4] = 0x01.toByte()
        bytes[5] = 0x00.toByte()
        bytes[6] = 0x06.toByte()
        try {
            var mac = EndoCmdUtils.getInstance().getMac()
            if(mac.isBlank()){
                mac = EndoCmdUtils.getInstance().getMacNew()
                mac = mac.substring(0,2)+":"+mac.substring(2,4)+":"+mac.substring(4,6)+":"+mac.substring(6,8)+":"+mac.substring(8,10)+":"+mac.substring(10,12)
            }
            logi("CmdEndoBroadCast $mac")
            val macList = mac.split(":")
            logi("CmdEndoBroadCast $macList")

            bytes[7] = Integer.parseInt(macList[0], 16).toByte()
            bytes[8] = Integer.parseInt(macList[1], 16).toByte()
            bytes[9] = Integer.parseInt(macList[2], 16).toByte()
            bytes[10] = Integer.parseInt(macList[3], 16).toByte()
            bytes[11] = Integer.parseInt(macList[4], 16).toByte()
            bytes[12] = Integer.parseInt(macList[5], 16).toByte()

            return bytes
        } catch (e: Exception) {
            loge("CmdEndoBroadCast $e")
        }
        return bytes
    }

    //重置测肤笔超时时间
    override fun getData(cmd: ByteArray): String? {
        EndoSocketService.lastHeartCmdTime = System.currentTimeMillis()
        logi("JporConnect EndoSocketService.lastTime ${EndoSocketService.lastHeartCmdTime}")
        return null
    }

}