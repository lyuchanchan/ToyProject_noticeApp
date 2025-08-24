package com.example.toyproject_noticeapp.data

// ❗️ Timestamp와 Date를 사용하기 위해 import 구문을 추가합니다.
import com.google.firebase.Timestamp
import java.util.Date

data class DataNotificationItem(
    val id: Int = 0,
    val category: String = "",
    val date: String = "",
    val title: String = "",
    val description: String = "",
    @JvmField
    var isFavorite: Boolean = false,
    val url: String = "",
    val viewCount: Int = 0,
    // ❗️ Firestore의 timestamp 필드를 받을 변수를 추가합니다.
    // 기본값으로 현재 시간을 넣어주어 안정성을 높입니다.
    val timestamp: Timestamp = Timestamp(Date())
)