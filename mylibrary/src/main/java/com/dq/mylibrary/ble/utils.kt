package com.dq.mylibrary.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.text.DecimalFormat

/**
 * create by zhi_liu on 2021/6/9
 */
//夜灯调节类型  延时/亮度/色温/无效参数
enum class ParamType {
    TYPE_DELAY, TYPE_LIGHT, TYPE_TEMPERATURE
}

//设备类型
enum class DevType(val value: Int) {
    TYPE_BRUSH(0);//牙刷

    companion object {
        fun getBedType(typeValue: Int): DevType {
            values().forEach {
                if (it.value == typeValue) {
                    return it
                }
            }
            return TYPE_BRUSH
        }
    }
}

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
