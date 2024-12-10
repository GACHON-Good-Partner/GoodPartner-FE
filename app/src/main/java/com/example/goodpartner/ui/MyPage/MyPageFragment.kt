package com.example.goodpartner.ui.MyPage

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.goodpartner.R
import com.example.goodpartner.databinding.FragmentMypageBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MyPageFragment : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View 바인딩 초기화
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 사용자 정보를 가져와 UI에 반영
        fetchUserInfo()

        binding.btnProfileChange.setOnClickListener {
            navigateToMyPageUDFragment()
        }




        return root
    }

    private fun navigateToMyPageUDFragment() {
        // MyPageUDFragment로 이동
        val fragment = MyPageUDFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_activity_main, fragment) // activity_main.xml에서 FragmentContainerView의 ID
            .addToBackStack(null) // 뒤로 가기를 통해 이전 화면으로 돌아갈 수 있도록 설정
            .commit()
    }

    /**
     * 사용자 정보를 백엔드에서 조회하여 UI에 반영하는 메서드
     */
    private fun fetchUserInfo() {
        // 저장된 Access Token을 SharedPreferences에서 가져오기
        val accessToken = requireContext()
            .getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) // Context.MODE_PRIVATE로 수정
            .getString("accessToken", null)

        // Access Token이 없을 경우 처리
        if (accessToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            Log.w("MyPageFragment", "Access Token이 없습니다. 로그인이 필요합니다.")
            return
        }

        // API 엔드포인트 URL
        val url = "https://13.209.81.42.nip.io/users" // 실제 서버 주소로 변경 필요
        // OkHttp 클라이언트 설정
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // 연결 타임아웃 설정
            .writeTimeout(10, TimeUnit.SECONDS)  // 쓰기 타임아웃 설정
            .readTimeout(10, TimeUnit.SECONDS)   // 읽기 타임아웃 설정
            .build()

        // 요청 객체 생성
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken") // JWT 토큰 추가
            .build()

        Log.i("MyPageFragment", "서버에 사용자 정보 요청을 시작합니다.")

        // 비동기 요청 실행
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 요청 실패 시 처리
                Log.e("MyPageFragment", "서버 요청 실패: ${e.localizedMessage}")
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "서버 요청에 실패했습니다. 네트워크 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.i("MyPageFragment", "서버 응답: $responseBody") // 서버 응답 전체를 로그에 출력

                if (response.isSuccessful && responseBody != null) {
                    try {
                        // 서버 응답 파싱
                        val jsonObject = JSONObject(responseBody)
                        val code = jsonObject.optInt("code")
                        val message = jsonObject.optString("message")

                        if (code == 200) {
                            val data = jsonObject.optJSONObject("data")
                            val userName = data?.optString("name", "알 수 없음")
                            val userEmail = data?.optString("email", "알 수 없음")
                            val userPhone = data?.optString("tel", "알 수 없음")

                            // UI 업데이트
                            requireActivity().runOnUiThread {
                                val displayedPhone = if (userPhone.isNullOrEmpty()) "전화번호 없음" else userPhone
                                binding.mpUnTv.text = "$userName"
                                binding.myUserName.text = "이름 : $userName"
                                binding.myUserEmail.text = "이메일 : $userEmail"
                                binding.myUserNumber.text = "전화번호: $displayedPhone"
                                Log.i("MyPageFragment", "사용자 정보를 성공적으로 UI에 반영했습니다.")
                            }

                        } else {
                            Log.e("MyPageFragment", "사용자 정보 조회 실패: $message")
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "사용자 정보 조회에 실패했습니다: $message", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MyPageFragment", "서버 응답 처리 중 오류 발생: ${e.localizedMessage}")
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "응답 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("MyPageFragment", "서버 응답 실패: 코드(${response.code}), 메시지(${response.message})")
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "서버 응답에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    /**
     * Fragment의 View가 파괴될 때 바인딩 리소스를 정리하는 메서드
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.i("MyPageFragment", "바인딩 리소스를 정리했습니다.")
    }
}
