package com.example.toyproject_noticeapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.toyproject_noticeapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 프래그먼트를 수동으로 띄우는 if 블록을 완전히 삭제합니다.
        // 이제부터는 activity_main.xml에 연결된 nav_graph가
        // 시작 화면(HomeMainFragment)을 자동으로 띄워줍니다.
    }
}