package com.dq.mylibrary.mvi.network

import com.dq.mylibrary.mvi.data.model.Wallpaper
import retrofit2.http.GET

interface ApiService {
    /**
     * 获取壁纸
     */
    @GET("v1/vertical/vertical?limit=30&skip=180&adult=false&first=0&order=hot")
    suspend fun getWallPaper(): Wallpaper
}