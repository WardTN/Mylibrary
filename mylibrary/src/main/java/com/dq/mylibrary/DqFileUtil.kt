package com.dq.mylibrary

import java.io.File


private const val HOME_PATH_NAME = "HomePat"



fun getHomePath(): String? {
    var homePath: String? = null
    try {
        val extStoragePath = DqAppInstance.getCurApplication().filesDir.absolutePath
        homePath = File(extStoragePath, HOME_PATH_NAME).canonicalPath
    } catch (e: Exception) {
        printStack(e)
    }
    return homePath
}

fun getSubDir(parent: String?, dir: String): String? {
    if (parent == null) return null
    var subDirPath: String? = null
    try {
        // 获取展开的子目录路径
        val subDirFile = File(parent, dir)
        if (!subDirFile.exists()) subDirFile.mkdirs()
        subDirPath = subDirFile.canonicalPath
    } catch (e: Exception) {
        printStack(e)
    }
    return subDirPath
}

