package com.example.toyproject_noticeapp.data

data class DataNotificationItem(
    val id: Int,
    val category: String,
    val date: String,
    val title: String,
    val description: String,
    var isFavorite: Boolean
)