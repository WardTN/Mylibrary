package com.dq.mylibrary.mvi.data.repository

import com.dq.mylibrary.mvi.network.ApiService

/**
 * 数据存储库
 */
class MainRepository(private val apiService: ApiService) {

    /**
     * 获取壁纸
     */
    suspend fun getWallPaper() = apiService.getWallPaper()
}