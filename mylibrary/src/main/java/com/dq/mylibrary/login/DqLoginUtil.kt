package com.dq.mylibrary.login

import android.content.Context
import android.text.TextUtils
import java.util.regex.Pattern

/**
 * 登录注册相关类
 */

fun isMobile(mobile: String?): Boolean {
    val REGEX_MOBILE = "^[1][3578][0-9]{9}$"
    return Pattern.matches(REGEX_MOBILE, mobile)
}


fun checkMobile(phone: String, context: Context): Int {
    if (TextUtils.isEmpty(phone)) {
//        ToastUtils.showLong("请输入手机号")
        return 0
    }

    if (!isMobile(phone)) {
//        ToastUtils.showLong(context.resources.getString(R.string.not_fit_phonenum))
        return 1
    }
    return 200
}

fun checkPwd(pwd: String, context: Context): Int {
    if (TextUtils.isEmpty(pwd)) {
//        ToastUtils.showLong(context.resources.getString(R.string.input_pass))
        return 0
    }

    if (pwd.length < 6) {
//        ToastUtils.showLong(context.resources.getString(R.string.password_at_least_length))
        return 1
    }

    return 200
}