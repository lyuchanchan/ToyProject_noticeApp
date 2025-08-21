package com.example.toyproject_noticeapp.ui.login // 패키지 이름은 본인 프로젝트에 맞게 확인하세요

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.toyproject_noticeapp.R

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // XML의 뷰(View)들과 코드 연결
        val etName = findViewById<EditText>(R.id.etName)
        val etId = findViewById<EditText>(R.id.etId)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etPasswordConfirm = findViewById<EditText>(R.id.etPasswordConfirm)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // '가입하기' 버튼 클릭 시 이벤트 처리
        btnSignup.setOnClickListener {
            // 사용자가 입력한 값들을 가져오기 (.trim()으로 불필요한 공백 제거)
            val name = etName.text.toString().trim()
            val id = etId.text.toString().trim()
            val pw = etPassword.text.toString().trim()
            val pwConfirm = etPasswordConfirm.text.toString().trim()

            // --- 유효성 검사 (Validation) --- //

            // 1. 모든 필드가 채워졌는지 확인
            if (name.isEmpty() || id.isEmpty() || pw.isEmpty() || pwConfirm.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // 검사를 통과하지 못했으므로 아래 로직을 실행하지 않고 종료
            }

            // 2. 비밀번호와 비밀번호 확인이 일치하는지 확인
            if (pw != pwConfirm) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 아이디 중복 확인, 비밀번호 보안 강도(6자리 이상 등) 검사 로직 추가 가능

            // --- 모든 검사를 통과했을 때 --- //

            // TODO: 실제로는 여기서 서버에 사용자 정보를 보내 회원가입을 요청해야 합니다.
            // 지금은 성공했다는 메시지만 띄우고 로그인 화면으로 돌아갑니다.

            Toast.makeText(this, "'$name'님, 회원가입 성공!", Toast.LENGTH_SHORT).show()

            // 회원가입 성공 후, 로그인 화면으로 돌아가기
            // Intent에 방금 가입한 아이디를 담아 전달하면 로그인 창에 자동으로 채워주는 경험을 줄 수 있습니다.
            val intent = Intent(this, LoginActivity::class.java).apply {
                putExtra("USER_ID", id)
            }
            startActivity(intent)
            finish() // 현재 회원가입 화면은 종료
        }

        // '로그인' 버튼 클릭 시 이벤트 처리
        btnLogin.setOnClickListener {
            // 현재 액티비티를 종료하여 이전 화면(로그인)으로 돌아감
            finish()
        }
    }
}