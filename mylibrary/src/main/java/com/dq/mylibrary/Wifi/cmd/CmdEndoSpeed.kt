package com.dq.mylibrary.Wifi.cmd

import com.solex.endo.midea.ui.event.EndoSpeedEvents
import com.solex.endo.midea.ui.service.EndoSocketService
import com.solex.endo.midea.util.CmdUtil
import com.solex.endo.midea.util.loge
import com.solex.endo.midea.util.utils
import org.greenrobot.eventbus.EventBus


/**
 *
 */
class CmdEndoSpeed : EndoBaseCmd() {

    override fun initCmd(data: String): ByteArray {
        var bytes = ByteArray(16)
        getCommon(bytes)
        bytes[4] = 0x05.toByte()
        bytes[5] = 0x00.toByte()
        bytes[6] = 0x01.toByte()

        bytes[7] = Integer.parseInt(data).toByte()

        if(utils.Dev_TYPE == 2) {
            //二代冲牙才需要加密
            var CmdUtil = CmdUtil()
            bytes = CmdUtil.MsgWith(bytes,8, byteArrayOf(bytes[4], bytes[5], bytes[6],bytes[7]))
        }

        return bytes
    }

    override fun getData(cmd: ByteArray): String? {
        val speed = cmd[7].toInt()
        loge("CmdEndoSpeed speed $speed")
        EventBus.getDefault().post(EndoSpeedEvents(speed))
        EndoSocketService.speed = speed
        return null
    }

}