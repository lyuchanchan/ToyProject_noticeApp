package com.example.toyproject_noticeapp.data

// 하나의 알림 항목을 나타내는 데이터 클래스
data class DataNotificationItem(
    val category: String,
    val date: String,
    val title: String,
    val description: String,
    val isFavorite: Boolean
)