package com.example.toyproject_noticeapp.data

data class Notice(
    val category: String,
    val title: String,
    val date: String,
    val isNew: Boolean = false
)