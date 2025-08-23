package com.example.toyproject_noticeapp.data

data class DataNotificationItem(
    val id: Int = 0,
    val category: String = "",
    val date: String = "",
    val title: String = "",
    val description: String = "",
    @JvmField
    var isFavorite: Boolean = false,
    val url: String = "",
    val viewCount: Int = 0 // ⬅️ 조회수 필드 추가
)