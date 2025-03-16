package com.dq.mylibrary.ble

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import java.text.DecimalFormat



fun roundByScale(v: Double, scale: Int): String {
    if (scale < 0) {
        throw  IllegalArgumentException("The   scale   must   be   a   positive   integer   or   zero")
    }
    if (scale == 0) {
        return DecimalFormat("0").format(v)
    }
    var formatStr = "0."

    for (i in 0 until scale) {
        formatStr += "0"
    }
    return DecimalFormat(formatStr).format(v);
}

/**
 * 判断是否有蓝牙权限
 */
fun isHasBlePermission(ctx: Context) = ContextCompat.checkSelfPermission(ctx,
    Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED


//fun isDBEmpty(): Boolean {
//    val devList = getDevList()
//    return devList.size == 0
//}
/**
 * 设置sp数据
 */
fun setSpData(context: Context, Tag: String, devId: String) {
    val sp = context.getSharedPreferences("HomeData", Context.MODE_PRIVATE)
    val editor = sp.edit()
    editor.putString(Tag, devId)
    editor.apply()
}

/**
 * 获取sp数据
 */
fun getSpData(context: Context, Tag: String): String {
    val sp = context.getSharedPreferences("HomeData", Context.MODE_PRIVATE)
    return sp.getString(Tag, "0")!!
}


// 权限检查扩展函数
@RequiresApi(Build.VERSION_CODES.M)
fun Activity.checkBlePermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    } else {
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}

