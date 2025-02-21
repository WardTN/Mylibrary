package com.tn.myapplication

import com.dq.mylibrary.ble.BaseBleManager

class BleMsgManager : BaseBleManager() {

    val UUID_SERVICE = "20190d0c-0b0a-0908-0706-050403020100"
    val UUID_WRITE = "212b0d0c-0b0a-0908-0706-050403020100"
    val UUID_NOTIFY = "202b0d0c-0b0a-0908-0706-050403020100"


    override fun getServiceUUID(): String {
        return UUID_SERVICE
    }

    override fun getWriteUUID(): String {
        return UUID_WRITE
    }

    override fun getNotifyUUID(): String {
        return UUID_NOTIFY
    }

    override fun parseNotifyData(data: ByteArray) {

    }


    override fun bleDisConnect() {

    }


}