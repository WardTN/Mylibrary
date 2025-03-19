package com.tn.myapplication

import android.app.Dialog
import android.content.Context
import android.os.Bundle

class LoadingDialog(context: Context, cancelable: Boolean): Dialog(context) {

    init {
        setCancelable(cancelable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_loading)
    }
}