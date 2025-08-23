package com.example.toyproject_noticeapp.data

import androidx.annotation.DrawableRes

data class Shortcut(
    @DrawableRes val iconResId: Int,
    val name: String,
    val url: String // ⬅️ URL 정보를 저장할 변수를 추가합니다.
)