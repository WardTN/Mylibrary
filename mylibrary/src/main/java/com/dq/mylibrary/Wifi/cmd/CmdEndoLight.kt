package com.dq.mylibrary.Wifi.cmd



/**
 *
 * @Description: java类作用描述
 * @Author: Jpor
 * @CreateDate: 2021/3/2 10:42
 *
 */
class CmdEndoLight : EndoBaseCmd() {

    companion object{
        const val open_2 = "3"  //UV灯光
        const val open_1 = "2"  //偏振灯光
        const val open = "1"  //普通灯光
        const val close = "0" //关闭灯光
    }

    override fun initCmd(data: String): ByteArray {
        var bytes = ByteArray(16)
        getCommon(bytes)
        bytes[4] = 0x06.toByte()
        bytes[5] = 0x00.toByte()
        bytes[6] = 0x01.toByte()

        bytes[7] = Integer.parseInt(data).toByte()

//        Log.e("2222","LIGHT加密前："+ EndoSocketUtils.bytesToHexString(bytes))
//        if(utils.Dev_TYPE == 2) {
//            //二代冲牙才需要加密
//            var CmdUtil = CmdUtil()
//            bytes = CmdUtil.MsgWith(bytes,8, byteArrayOf(bytes[4], bytes[5], bytes[6],bytes[7]))
//        }
//        Log.e("2222","LIGHT加密后："+ EndoSocketUtils.bytesToHexString(bytes))
        return bytes
    }

    override fun getData(cmd: ByteArray): String? {
        return null
    }

}