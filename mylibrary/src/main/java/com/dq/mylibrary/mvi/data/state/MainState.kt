package com.dq.mylibrary.mvi.data.state

import com.dq.mylibrary.mvi.data.model.Wallpaper

/**
 * 页面状态
 */
sealed class MainState {
    /**
     * 空闲
     */
    object Idle : MainState()

    /**
     * 加载
     */
    object Loading : MainState()

    /**
     * 获取壁纸
     */
    data class Wallpapers(val wallpaper: Wallpaper) : MainState()

    /**
     * 错误信息
     */
    data class Error(val error: String) : MainState()
}