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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Access Token 존재 여부 확인
        val accessToken = getAccessToken()
        if (!accessToken.isNullOrEmpty()) {
            // Access Token이 있으면 MainActivity로 이동
            moveToMainActivity()
            return
        }

        // 액션바 숨기기
        supportActionBar?.hide()

        val kakaoLoginButton = findViewById<ImageButton>(R.id.kakao_login_button)
        val kakaoLogoutButton = findViewById<ImageButton>(R.id.kakao_logout_button)


        kakaoLoginButton.setOnClickListener {
            // 로그: 로그인 시작
            Log.i("LoginActivity", "카카오 로그인 시작")

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
                // 카카오톡 앱을 통한 로그인
                UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                    if (error != null) {
                        // 로그인 실패 로그
                        Log.e("LoginActivity", "카카오톡 로그인 실패", error)
                        handleLoginError(error)
                    } else if (token != null) {
                        // 로그인 성공 로그
                        Log.i("LoginActivity", "카카오톡 로그인 성공: ${token.accessToken}")
                        sendAuthCodeToBackend(token.accessToken)
                    }
                }
            } else {
                // 카카오 계정을 통한 로그인
                UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                    if (error != null) {
                        // 로그인 실패 로그
                        Log.e("LoginActivity", "카카오 계정 로그인 실패", error)
                        handleLoginError(error)
                    } else if (token != null) {
                        // 로그인 성공 로그
                        Log.i("LoginActivity", "카카오 계정 로그인 성공: ${token.accessToken}")
                        sendAuthCodeToBackend(token.accessToken)
                    }
                }
            }
        }

        kakaoLogoutButton.setOnClickListener {
            // 카카오 로그아웃 처리
            UserApiClient.instance.logout { error ->
                if (error != null) {
                    Log.e("LoginActivity", "카카오 로그아웃 실패", error)
                    Toast.makeText(this, "카카오 로그아웃에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.i("LoginActivity", "카카오 로그아웃 성공")

                    // SharedPreferences에서 토큰 삭제
                    clearAccessToken()

                    Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

                    // 필요하면 로그인 화면으로 이동
                    moveToLoginScreen()
                }
            }
        }


    }

    private fun handleLoginError(error: Throwable) {
        // 에러의 상세 원인을 확인
        if (error is com.kakao.sdk.common.model.ClientError) {
            Log.e("LoginActivity", "클라이언트 오류: ${error.reason}", error)
            when (error.reason) {
                com.kakao.sdk.common.model.ClientErrorCause.Cancelled -> {
                    Toast.makeText(this, "사용자가 로그인 취소", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "알 수 없는 클라이언트 오류 발생", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (error is com.kakao.sdk.common.model.AuthError) {
            Log.e("LoginActivity", "인증 오류: ${error.statusCode} - ${error.reason}", error)
            Toast.makeText(this, "인증 오류 발생: ${error.message}", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("LoginActivity", "예상치 못한 오류 발생: ${error.message}", error)
            Toast.makeText(this, "예상치 못한 오류: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private var lastAuthCode: String? = null

    private fun sendAuthCodeToBackend(authCode: String) {

        synchronized(this) {
            if (lastAuthCode == authCode) {
                Log.w("LoginActivity", "이미 전송된 authCode입니다.")
                return
            }
            lastAuthCode = authCode
            Log.i("LoginActivity", "서버로 전송할 authCode: $authCode")
        }

        // 로그: 서버로 authCode 전송
        Log.i("LoginActivity", "서버로 authCode 전송: $authCode")

        // 서버 URL 설정
        val url = "https://13.209.81.42.nip.io/users/social-login" // 서버 엔드포인트

        // JSON 요청 본문 생성
        val jsonRequestBody = """
        {
            "authCode": "$authCode"
        }
    """.trimIndent()

        // RequestBody 생성
        val requestBody = jsonRequestBody.toRequestBody("application/json".toMediaType())

        // HTTP 요청 생성
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        // OkHttp 클라이언트 생성
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        // 비동기 요청
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 서버 요청 실패 시 처리
                Log.e("LoginActivity", "서버 요청 실패", e)
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "서버 요청에 실패했습니다. 네트워크를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    try {
                        // JSON으로 파싱 시도
                        val json = JSONObject(responseBody)
                        val code = json.optInt("code")
                        val message = json.optString("message")

                        if (code == 200 && message == "소셜 로그인에 성공했습니다.") {
                            Log.i("LoginActivity", "로그인 성공: $responseBody")

                            // Access Token 및 Refresh Token 저장
                            val data = json.optJSONObject("data")
                            val accessToken = data?.optString("accessToken")

                            if (accessToken != null) {
                                saveAccessToken(accessToken)
                                Log.i("LoginActivity", "Access Token 저장 완료")
                            }

                            runOnUiThread {
                                moveToMainActivity()
                            }
                        } else {
                            Log.e("LoginActivity", "로그인 실패: $responseBody")
                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, "로그인 실패: $message", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "응답 파싱 실패, HTML 반환: $responseBody", e)
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "서버에서 예기치 않은 응답이 반환되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }   else {
                    // 서버 오류 처리
                    Log.e("LoginActivity", "서버 응답 실패: ${response.code} - ${response.message}")
                    Log.e("LoginActivity", "서버 응답 본문: $responseBody")

                    // 400 Bad Request 처리
                    if (response.code == 400 && responseBody.contains("invalid_grant")) {
                        runOnUiThread {
                            Toast.makeText(
                                this@LoginActivity,
                                "이미 사용된 Authorization Code입니다. 다시 로그인해주세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.w("LoginActivity", "Authorization Code 재사용 또는 만료됨. 새 로그인 필요.")
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@LoginActivity,
                                "서버 응답 실패: ${response.code}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        })
    }

    private fun saveAccessToken(accessToken: String) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("accessToken", accessToken)
        editor.apply()
        Log.i("LoginActivity", "Access Token 저장 완료 : $accessToken")
    }


    private fun clearAccessToken() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("accessToken") // Access Token 제거
        editor.apply()
        Log.i("LoginActivity", "Access Token 삭제 완료")
    }

    private fun moveToLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun getAccessToken(): String? {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        return sharedPreferences.getString("accessToken", null)
    }

    private fun moveToMainActivity() {
        // MainActivity로 화면 전환
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // LoginActivity 종료
    }
}