package com.example.toyproject_noticeapp.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.toyproject_noticeapp.MainActivity
import com.example.toyproject_noticeapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    // Firebase 인증 객체 선언
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase 인증 객체 초기화
        auth = Firebase.auth

        // 앱 시작 시, 이미 로그인된 사용자인지 확인
        if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etId.text.toString().trim()
            val userPassword = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, userPassword)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        //  바로 로그인이 아닌, 이메일 인증 여부를 확인합니다.
                        if (user != null && user.isEmailVerified) {
                            // 인증된 사용자만 메인 화면으로 이동
                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // 인증되지 않은 사용자는 로그인을 막습니다.
                            Toast.makeText(this, "가입 시 발송된 이메일을 확인하여 인증을 완료해주세요.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // 아이디나 비밀번호가 틀렸을 때
                        Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}