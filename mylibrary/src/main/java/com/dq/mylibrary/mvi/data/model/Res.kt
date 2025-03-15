package com.dq.mylibrary.mvi.data.model

import com.squareup.moshi.Json

data class Res(
    @Json(name = "vertical")
    val vertical: List<Vertical>
)