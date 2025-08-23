package com.example.toyproject_noticeapp.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.toyproject_noticeapp.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        val db = Firebase.firestore

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etId.text.toString().trim()
            val pw = binding.etPassword.text.toString().trim()
            val pwConfirm = binding.etPasswordConfirm.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || pw.isEmpty() || pwConfirm.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pw != pwConfirm) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pw)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val userProfile = hashMapOf("name" to name)

                            // Firestore에 사용자 정보를 저장
                            db.collection("users").document(user.uid).set(userProfile)
                                .addOnSuccessListener {
                                    // Firestore 저장이 성공하면 다음 단계 진행
                                    user.sendEmailVerification()
                                        .addOnCompleteListener { sendTask ->
                                            if (sendTask.isSuccessful) {
                                                Toast.makeText(this, "인증 메일을 발송했습니다. 이메일을 확인해주세요.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(this, "인증 메일 발송에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                    FirebaseAuth.getInstance().signOut()
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.putExtra("USER_ID", email)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    // Firestore 저장 실패 시, 방금 생성된 계정 삭제
                                    user.delete()
                                    Toast.makeText(this, "회원가입 실패: 프로필 정보 저장 오류. (${e.message})", Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.btnLogin.setOnClickListener {
            finish()
        }
    }
}