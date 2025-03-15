package com.dq.mylibrary.Wifi.cmd

import com.solex.endo.midea.ui.event.EndoSetWiFiEvents
import com.solex.endo.midea.ui.service.EndoSocketService
import com.solex.endo.midea.util.CmdUtil
import com.solex.endo.midea.util.utils
import org.greenrobot.eventbus.EventBus


/**
 *
 * @Description: java类作用描述
 * @Author: Jpor
 * @CreateDate: 2021/3/2 10:42
 *
 */
class CmdEndoWiFiSSID : EndoBaseCmd() {

    override fun initCmd(ssid: String): ByteArray {
        var bytes = ByteArray(7 + ssid.length)
        getCommon(bytes)
        bytes[4] = 0x03.toByte()
        bytes[5] = 0x00.toByte()
        bytes[6] = ssid.length.toByte()

        var cycbytes = ByteArray(3 + ssid.length)
        cycbytes[0] = bytes[4]
        cycbytes[1] = bytes[5]
        cycbytes[2] = bytes[6]
        for (i in ssid.indices) {
            bytes[7 + i] = ssid[i].toByte()
            cycbytes[3 + i] = ssid[i].toByte()
        }


//        Log.e("2222","WIFI_SSID加密前："+ EndoSocketUtils.bytesToHexString(bytes))
        if(utils.Companion.Dev_TYPE == 2) {
            //二代冲牙才需要加密
            var CmdUtil = CmdUtil()
            bytes = CmdUtil.MsgWith(bytes,7 + ssid.length, cycbytes)
        }
//        Log.e("2222","WIFI_SSID加密后："+ EndoSocketUtils.bytesToHexString(bytes))

        return bytes
    }

    override fun getData(cmd: ByteArray): String? {
        EndoSocketService.setWiFiSSID = true
        if (EndoSocketService.setWiFiPWD) {
            EventBus.getDefault().post(EndoSetWiFiEvents(true))
        }
        return null
    }

}