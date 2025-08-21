package com.example.toyproject_noticeapp.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.toyproject_noticeapp.MainActivity
import com.example.toyproject_noticeapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedUserId = sharedPreferences.getString("user_id", null)

        if (savedUserId != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val userId = binding.etId.text.toString().trim()
            val userPassword = binding.etPassword.text.toString().trim()

            if (userId.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId == "test" && userPassword == "test") {
                sharedPreferences.edit().putString("user_id", userId).apply()
                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "아이디 또는 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // ##### 이 부분이 추가되었습니다! #####
        // 개발용 Skip 버튼 클릭 리스너
        binding.tvSkip.setOnClickListener {
            sharedPreferences.edit().putString("user_id", "dev_user").apply()
            Toast.makeText(this, "테스트 모드로 메인 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}