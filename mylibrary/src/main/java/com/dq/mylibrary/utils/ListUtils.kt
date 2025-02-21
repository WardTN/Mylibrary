package com.dq.mylibrary.utils



fun <T> listIsEmpty(list: List<T>?): Boolean {
    if (list == null || list.isEmpty()) {
        return true
    }
    return false
}


