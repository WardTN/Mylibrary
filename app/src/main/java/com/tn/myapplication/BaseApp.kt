package com.tn.myapplication

import android.app.Application
import com.dq.mylibrary.ble.BleDevBean
import com.dq.mylibrary.ble.BlePrepare
import kotlin.properties.Delegates



const val BLE_NAME_Brush = "pelvicfloor"//哑铃
//扫描超时时间
const val SCAN_TIME_OUT = 3000L
//最大扫描次数
const val MAX_SCAN_COUNT = 3


open class BaseApp : Application() {
    companion object {
        var instance: BaseApp by Delegates.notNull()
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val bleBean = BleDevBean(BLE_NAME_Brush,SCAN_TIME_OUT,MAX_SCAN_COUNT)
        BlePrepare.instance.initBle(this,bleBean)
//        CrashApphandler.getInstance().init(this) //捕获异常

    }
}
