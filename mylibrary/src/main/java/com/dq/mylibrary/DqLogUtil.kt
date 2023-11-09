package com.dq.mylibrary

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.Exception


fun dqLog(msg: String) {
    Log.e("CHEN", msg)
}

fun printStack(e: Exception) {
    e.printStackTrace()
}



var logSavePath = "${getHomePath()}/dqlog.txt"
/**
 * 存储log 进本地txt
 */
fun logSave(string: String){
    try {
        var file = File(logSavePath)
        val writer = FileWriter(file, true)
        writer.write(string + "\n")
        writer.flush()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

/**
 * 删除 log.txt
 */
fun delLogTxt(){
    var file = File(logSavePath)
    if (file.exists()) {
        file.mkdirs()
    }
}





