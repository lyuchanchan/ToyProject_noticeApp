package com.example.toyproject_noticeapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.toyproject_noticeapp.databinding.ActivityMainBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    // ❗️ 알림 권한 요청을 위한 ActivityResultLauncher (새로 추가)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "알림 권한이 거부되었습니다. 설정에서 직접 허용할 수 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ❗️ 알림 권한 요청 함수 호출 (새로 추가)
        askNotificationPermission()
        // ❗️ 토픽 구독 및 토큰 확인 코드 (수정됨)
        setupFirebase()
    }

    // ❗️ 알림 권한을 요청하는 함수 (새로 추가)
    private fun askNotificationPermission() {
        // Android 13 (API 33) 이상에서만 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // 권한이 이미 있으면 아무것도 하지 않음
            } else {
                // 권한이 없으면 요청
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // ❗️ Firebase 관련 설정을 모아두는 함수 (수정됨)
    private fun setupFirebase() {
        // 1. 'new_notice' 토픽 구독
        Firebase.messaging.subscribeToTopic("new_notice")
            .addOnCompleteListener { task ->
                var msg = "✅ Subscribed to new_notice topic"
                if (!task.isSuccessful) {
                    msg = "❌ Failed to subscribe to new_notice topic"
                }
                Log.d(TAG, msg)
            }

        // ❗️ 현재 FCM 토큰을 가져와 Firestore에 저장
        Firebase.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d(TAG, "Current FCM Token: $token")

            // Firestore에 토큰 저장
            val uid = Firebase.auth.currentUser?.uid
            if (uid != null) {
                Firebase.firestore.collection("users").document(uid)
                    .update("fcmToken", token)
            }
        }
    }
}