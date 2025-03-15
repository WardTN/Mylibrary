package com.dq.mylibrary.Wifi.cmd

import android.util.Log
import com.solex.endo.midea.ui.event.SkinPicEvents
import com.solex.endo.midea.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class CmdEndoSkinLight : EndoBaseCmd() {
    companion object{
        const val open_2 = "3"  //UV灯光
        const val open_1 = "2"  //偏振灯光
        const val open = "1"  //普通灯光
        const val close = "0" //关闭灯光
        var historyOldTime : Int = 0//累计次数
    }

    override fun initCmd(data: String): ByteArray {
        var bytes = ByteArray(16)
        getCommon(bytes)
        bytes[4] = 0x05.toByte()
        bytes[5] = 0x00.toByte()
        bytes[6] = 0x01.toByte()

        bytes[7] = Integer.parseInt(data).toByte()

        Log.e("2222","LIGHT加密前："+ EndoSocketUtils.bytesToHexString(bytes))
        if(utils.Dev_TYPE == 2) {
            //二代冲牙才需要加密
            var CmdUtil = CmdUtil()
            bytes = CmdUtil.MsgWith(bytes,8, byteArrayOf(bytes[4], bytes[5], bytes[6],bytes[7]))
        }
        Log.e("2222","LIGHT加密后："+ EndoSocketUtils.bytesToHexString(bytes))
        return bytes
    }

    override fun getData(cmd: ByteArray): String? {
        historyOldTime++
        if(historyOldTime == 1){
            EventBus.getDefault().post(SkinPicEvents(true)).toString()//手柄拍照开关
            GlobalScope.launch {
                delay(5000)
                historyOldTime = 0
            }
        }
        return null
    }

}