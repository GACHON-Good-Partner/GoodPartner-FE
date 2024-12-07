package com.example.goodpartner.Login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.goodpartner.MainActivity
import com.example.goodpartner.R
import com.kakao.sdk.user.UserApiClient

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 카카오 로그인 버튼
        val kakaoLoginButton = findViewById<ImageButton>(R.id.kakao_login_button)

        kakaoLoginButton.setOnClickListener {
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                // 카카오톡 앱을 통한 로그인
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    handleLoginResult(token?.accessToken, error)
                }
            } else {
                // 카카오 계정을 통한 로그인
                UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                    handleLoginResult(token?.accessToken, error)
                }
            }
        }
    }

    private fun handleLoginResult(accessToken: String?, error: Throwable?) {
        if (error != null) {
            Log.e("LoginActivity", "로그인 실패", error)
            Toast.makeText(this, "로그인에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
        } else if (accessToken != null) {
            Log.i("LoginActivity", "로그인 성공! 토큰: $accessToken")

            // 사용자 정보 가져오기
            UserApiClient.instance.me { user, userError ->
                if (userError != null) {
                    Log.e("LoginActivity", "사용자 정보 요청 실패", userError)
                    Toast.makeText(this, "사용자 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                } else if (user != null) {
                    Log.i("LoginActivity", "사용자 정보 요청 성공: ${user.kakaoAccount?.profile?.nickname}")

                    // MainActivity로 이동
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("nickname", user.kakaoAccount?.profile?.nickname)
                    intent.putExtra("email", user.kakaoAccount?.email)
                    startActivity(intent)
                    finish() // LoginActivity 종료
                }
            }
        }
    }
}
