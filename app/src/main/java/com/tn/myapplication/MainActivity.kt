package com.tn.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dq.mylibrary.dqLog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dqLog("Hello world")
    }
}