package com.example.toyproject_noticeapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.toyproject_noticeapp.databinding.ActivityMainBinding
import com.example.toyproject_noticeapp.ui.home.HomeMainFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 앱이 처음 시작될 때만 HomeMainFragment를 표시합니다.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, HomeMainFragment())
                .commit()
        }
    }
}