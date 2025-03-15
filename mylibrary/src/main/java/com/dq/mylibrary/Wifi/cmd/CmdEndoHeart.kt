package com.dq.mylibrary.Wifi.cmd

import android.util.Log
import com.solex.endo.midea.ui.event.EndoBatteryEvents
import com.solex.endo.midea.ui.event.notifyWater
import com.solex.endo.midea.ui.service.EndoSocketService
import com.solex.endo.midea.util.*
import com.solex.endo.midea.util.EndoSocketUtils.byteToInt
import org.greenrobot.eventbus.EventBus


/**
 *
 * @Description: java类作用描述
 * @Author: Jpor
 * @CreateDate: 2021/3/2 10:42
 *
 */
class CmdEndoHeart : EndoBaseCmd() {

    override fun initCmd(data: String): ByteArray {
        var bytes = ByteArray(16)
        getCommon(bytes)
        bytes[4] = 0x02.toByte()
        bytes[5] = 0x00.toByte()
        bytes[6] = 0x00.toByte()
        Log.e("2222", "心跳加密前HHH：" + EndoSocketUtils.bytesToHexString(bytes))
        if (utils.Dev_TYPE == 2) {
            //二代冲牙才需要加密
            var CmdUtil = CmdUtil()
            bytes = CmdUtil.MsgWith(bytes, 7, byteArrayOf(bytes[4], bytes[5], bytes[6]))
        }
        Log.e("2222", "心跳加密后：" + EndoSocketUtils.bytesToHexString(bytes))
        return bytes
    }

    //重置测肤笔超时时间
    override fun getData(cmd: ByteArray): String? {
        try {
            EndoSocketService.lastHeartCmdTime = System.currentTimeMillis()
//            logi("isWiFiConnect lastTime ${EndoSocketService.lastHeartCmdTime}")
//            logi("JporConnect lastTime ${EndoSocketService.lastHeartCmdTime}")
            val battery = byteToInt(cmd[7], cmd[8])//电量
            val tempBattery = getBatteryValue(battery)
            if (tempBattery < 101) {
                EndoSocketService.battery = tempBattery
            }

            val reCharge = cmd[9].toInt()
            var water_H = byteToInt(cmd[10])
            var water_L = byteToInt(cmd[11])
            if (water_H < 0) {
                water_H = -water_H
            }
            if (water_L < 0) {
                water_L = -water_L
            }
            var water = (water_H * 255 + water_L)
//            Log.e("3333", "水值1: " + water_H + "水值2:"+water_L)
//            Log.e("3333", "水值：$water_H,$water_L=$water,,,电量：$tempBattery")
            SolexLogUtil.dq_log("水值：$water_H,$water_L=$water,,,电量：$tempBattery"+ "传输Battery" + battery)
            EventBus.getDefault().post(notifyWater(water))
            EventBus.getDefault().post(EndoBatteryEvents(EndoSocketService.battery, reCharge == 1))
        } catch (e: Exception) {
            printStack(e)
        }
        return null
    }

}