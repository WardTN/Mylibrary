package com.dq.mylibrary.base

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding


abstract class BaseActivity<DB : ViewDataBinding> : AppCompatActivity() {

    lateinit var databinding: DB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootView = View.inflate(this, getViewId(), null)
        setContentView(rootView)
        databinding = DataBindingUtil.bind(rootView)!!
        databinding.lifecycleOwner = this

        initView()
        initClick()
    }

    abstract fun getViewId(): Int

    open fun initView() {}

    open fun initClick() {}

    /**
     * 设置屏幕旋转
     */
    private fun setScreenRotation() {
        requestedOrientation = if (getRotation()) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }


    open fun getRotation(): Boolean {
        return true
    }

}