package com.example.toyproject_noticeapp.data

data class DataNotificationItem(
    val id: Int = 0,
    val category: String = "",
    val date: String = "",
    val title: String = "",
    val description: String = "",
    @JvmField // ⬅️ 이 줄을 추가하세요.
    var isFavorite: Boolean = false,
    val url: String = ""
)