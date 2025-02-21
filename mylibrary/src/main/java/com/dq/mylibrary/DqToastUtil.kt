package com.dq.mylibrary

import android.widget.Toast
import com.blankj.utilcode.util.ToastUtils


fun doToastShort(title:String){
    Toast.makeText(DqAppInstance.getCurApplication(), title, Toast.LENGTH_SHORT).show()
}


fun doToastLong(title:String){
//    Toast.makeText(DqAppInstance.getCurApplication(), title, Toast.LENGTH_LONG).show()
}