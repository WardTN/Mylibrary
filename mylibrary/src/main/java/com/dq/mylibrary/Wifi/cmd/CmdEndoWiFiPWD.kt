package com.dq.mylibrary.Wifi.cmd


import org.greenrobot.eventbus.EventBus


/**
 *
 * @Description: java类作用描述
 * @Author: Jpor
 * @CreateDate: 2021/3/2 10:42
 *
 */
class CmdEndoWiFiPWD : EndoBaseCmd() {

    override fun initCmd(data: String): ByteArray {
        var bytes = ByteArray(7 + data.length)
        getCommon(bytes)
        bytes[4] = 0x04.toByte()
        bytes[5] = 0x00.toByte()
        bytes[6] = data.length.toByte()

        var cycbytes = ByteArray(3 + data.length)
        cycbytes[0] = bytes[4]
        cycbytes[1] = bytes[5]
        cycbytes[2] = bytes[6]

        for (i in data.indices) {
            bytes[7 + i] = data[i].toByte()
            cycbytes[3 + i] = data[i].toByte()
        }

//        Log.e("2222","WIFI_PWD加密前："+ EndoSocketUtils.bytesToHexString(bytes))
//        if(utils.Companion.Dev_TYPE == 2) {
//            //二代冲牙才需要加密
//            var CmdUtil = CmdUtil()
//            bytes = CmdUtil.MsgWith(bytes,7 + data.length, cycbytes)
//        }
//        Log.e("2222","WIFI_PWD加密后："+ EndoSocketUtils.bytesToHexString(bytes))
        return bytes
    }

    override fun getData(cmd: ByteArray): String? {
//        setWiFiPWD = true
//        if (setWiFiSSID){
//            EventBus.getDefault().post(EndoSetWiFiEvents(true))
//        }
        return null
    }

}