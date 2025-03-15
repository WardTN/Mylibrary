package com.dq.mylibrary.Wifi.cmd


/**
 *
 * @Description: java类作用描述
 * @Author: Jpor
 * @CreateDate: 2021/3/2 10:42
 *
 */
class CmdEndoNone : EndoBaseCmd() {


    override fun initCmd(data: String): ByteArray {
        val bytes = ByteArray(16)
        getCommon(bytes)
        return bytes
    }

    override fun getData(cmd: ByteArray): String? {
        return null
    }

}