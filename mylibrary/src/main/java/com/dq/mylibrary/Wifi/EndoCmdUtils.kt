package com.dq.mylibrary.Wifi

import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.StringUtils
import com.dq.mylibrary.Constant.ENDO_SP_LOCAL_MAC
import com.dq.mylibrary.Constant.ENDO_SP_TAG
import com.dq.mylibrary.Wifi.cmd.CmdEndoBroadCast
import com.dq.mylibrary.Wifi.cmd.CmdEndoClose
import com.dq.mylibrary.Wifi.cmd.CmdEndoHeart
import com.dq.mylibrary.Wifi.cmd.CmdEndoLight
import com.dq.mylibrary.Wifi.cmd.CmdEndoNone
import com.dq.mylibrary.Wifi.cmd.CmdEndoSkinLight
import com.dq.mylibrary.Wifi.cmd.CmdEndoSpeed
import com.dq.mylibrary.Wifi.cmd.CmdEndoWiFiPWD
import com.dq.mylibrary.Wifi.cmd.CmdEndoWiFiSSID
import com.dq.mylibrary.Wifi.cmd.EndoCmdReceiveType
import java.net.DatagramPacket

/**
 * CMD 指令工具类
 */
class EndoCmdUtils {

    companion object {
        @Volatile
        private var instance: EndoCmdUtils? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: EndoCmdUtils().also { instance = it }
        }

        private var mac: String? = null
    }

    fun getMac(): String {
        if (mac == null) {
            mac = SPUtils.getInstance(ENDO_SP_TAG).getString(ENDO_SP_LOCAL_MAC, "")
            if (StringUtils.isEmpty(mac)) {
                mac = DeviceUtils.getMacAddress()
                SPUtils.getInstance(ENDO_SP_TAG).put(ENDO_SP_LOCAL_MAC, mac)
            }
        }
        mac?.let { return it }
        if (DeviceUtils.getMacAddress().isBlank()) {
//            SolexLogUtil.dq_log("当前Mac 为空")
        }
        return DeviceUtils.getMacAddress()
    }

    fun getMacNew(): String {
        return DeviceUtils.getUniqueDeviceId()
    }

    fun sendCmd(sendType: EndoCmdSendType, data: String) {
        when (sendType) {
            EndoCmdSendType.BROADCAST -> CmdEndoBroadCast().sendCmdFirst(data)
            EndoCmdSendType.HEART -> CmdEndoHeart().sendCmd(data)
            EndoCmdSendType.WIFI_SSID -> CmdEndoWiFiSSID().sendCmd(data)
            EndoCmdSendType.WIFI_PWD -> CmdEndoWiFiPWD().sendCmd(data)
            EndoCmdSendType.SPEED -> CmdEndoSpeed().sendCmd(data)
            EndoCmdSendType.LIGHT -> CmdEndoLight().sendCmd(data)
            EndoCmdSendType.SkinLIGHT -> {
                CmdEndoSkinLight().sendCmd(data)
                CmdEndoSkinLight().sendCmd(data)
            }
        }
    }

    /**
     * 解析 接收到的 数据报
     */
    fun parseCmd(packet: DatagramPacket): String? {
        var cmd = packet.data
            //二代设备需加解密
//            var cmdUtil = CmdUtil()
//            cmd = cmdUtil.MsgRead(cmd)
//        Log.e("2222", "返回数据HH：" + EndoSocketUtils.bytesToHexString(cmd))
//        SolexLogUtil.dq_log( "返回数据HH：" + EndoSocketUtils.bytesToHexString(cmd))

        if (cmd.size < 5) {
//            SolexLogUtil.dq_log("parseCmd error")
            return null
        }

        if (!isEndoCmd(cmd)) {
//            SolexLogUtil.dq_log("parseCmd no Endo cmd")
            return null
        }

        return when (getReceiveType(cmd)) {
            EndoCmdReceiveType.NONE -> CmdEndoNone().getData(cmd)
            EndoCmdReceiveType.BROADCAST -> {
                EndoSocketUtils.ADDRESS = packet.address.hostAddress
                CmdEndoBroadCast().getData(cmd)
            }
            EndoCmdReceiveType.HEART -> {
                CmdEndoHeart().getData(cmd)
            }
            EndoCmdReceiveType.WIFI_SSID -> CmdEndoWiFiSSID().getData(cmd)
            EndoCmdReceiveType.WIFI_PWD -> CmdEndoWiFiPWD().getData(cmd)
            EndoCmdReceiveType.SPEED -> {
                CmdEndoSpeed().getData(cmd)
            }
            EndoCmdReceiveType.LIGHT -> CmdEndoSpeed().getData(cmd)
            EndoCmdReceiveType.CLOSE -> CmdEndoClose().getData(cmd)
            EndoCmdReceiveType.PIC -> CmdEndoSkinLight().getData(cmd)
        }
    }

    private fun isEndoCmd(bytes: ByteArray): Boolean {
        if (bytes[0] == 0x55.toByte()
            && bytes[1] == 0x66.toByte()
        ) {
            return true
        }
        return false
    }

    private fun getReceiveType(bytes: ByteArray): EndoCmdReceiveType {
        return when (bytes[4]) {
            0x01.toByte() -> EndoCmdReceiveType.BROADCAST
            0x02.toByte() -> EndoCmdReceiveType.HEART
            0x03.toByte() -> EndoCmdReceiveType.WIFI_SSID
            0x04.toByte() -> EndoCmdReceiveType.WIFI_PWD
            0x05.toByte() -> EndoCmdReceiveType.SPEED
            0x08.toByte() -> EndoCmdReceiveType.CLOSE
            0x10.toByte() -> EndoCmdReceiveType.PIC
            0x3d.toByte() -> EndoCmdReceiveType.PIC
            0x69.toByte() -> EndoCmdReceiveType.PIC
            0x06.toByte() -> EndoCmdReceiveType.PIC
            else -> EndoCmdReceiveType.NONE
        }
    }


}