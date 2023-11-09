package com.dq.mylibrary

import android.app.Application

object DqAppInstance {
    var application: Application? = null

    fun getCurApplication():Application{
        return application!!
    }
}