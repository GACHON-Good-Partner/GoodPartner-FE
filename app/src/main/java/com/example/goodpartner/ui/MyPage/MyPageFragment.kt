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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goodpartner.R
import com.example.goodpartner.databinding.FragmentMypageBinding
import com.example.goodpartner.ui.dashboard.ChatFragment
import com.example.goodpartner.ui.dashboard.ChatResponse
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
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

        // 최근 질문 로드
        fetchRecentQuestions()

        // 수정 버튼 클릭 이벤트
        binding.btnProfileChange.setOnClickListener {
            findNavController().navigate(R.id.action_myPageFragment_to_myPageUDFragment)
        }




        return root
    }

    private fun navigateToMyPageUDFragment() {
        findNavController().navigate(R.id.action_myPageFragment_to_myPageUDFragment)
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

    private fun fetchRecentQuestions() {
        val accessToken = requireContext()
            .getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            .getString("accessToken", null)

        if (accessToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            Log.w("MyPageFragment", "Access Token이 없습니다. 로그인이 필요합니다.")
            return
        }

        val url = "https://13.209.81.42.nip.io/chats/latest"
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("MyPageFragment", "서버 요청 실패: ${e.localizedMessage}")
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "서버 요청에 실패했습니다. 네트워크 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.i("MyPageFragment", "서버 응답: $responseBody")

                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val code = jsonObject.optInt("code")
                        val data = jsonObject.optJSONArray("data")

                        if (code == 200 && data != null) {
                            val recentQuestions = mutableListOf<ChatResponse>()
                            for (i in 0 until data.length()) {
                                val item = data.getJSONObject(i)
                                recentQuestions.add(
                                    ChatResponse(
                                        chatId = item.optLong("id"),
                                        userId = item.optString("userId"),
                                        message = item.optString("message"),
                                        status = item.optString("status"),
                                        createdAt = formatServerTimeToLocalTime(item.optString("createdAt") ?:"시간없음"),
                                        updatedAt = item.optString("updatedAt"),
                                        keywordResponses = null // 키워드 응답 없음
                                    )
                                )
                            }

                            requireActivity().runOnUiThread {
                                // RecyclerView에 데이터 바인딩
                                val adapter = RecentQuestionsAdapter(recentQuestions) { chatResponse ->
                                    navigateToChatFragment(chatResponse.chatId) // 클릭 시 이동 처리
                                }
                                binding.recentQuestionsRecyclerView.apply {
                                    this.adapter = adapter
                                    this.layoutManager = LinearLayoutManager(context)
                                }
                            }

                            Log.d("MyPageFragment", "최근 질문 개수: ${recentQuestions.size}")
                            recentQuestions.forEach { question ->
                                Log.d("MyPageFragment", "질문 내용: ${question.message}")
                            }

                        } else {
                            Log.e("MyPageFragment", "최근 질문 조회 실패")
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "최근 질문 조회에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MyPageFragment", "JSON 파싱 오류: ${e.localizedMessage}")
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "서버 응답 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
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


    private fun navigateToChatFragment(chatId: Long) {
        val fragment = ChatFragment().apply {
            arguments = Bundle().apply {
                putLong("chatId", chatId)
            }
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_activity_main, fragment)
            .addToBackStack(null)
            .commit()
    }


    /**
     * 서버에서 받아온 시간 형식변환
     */
    private fun formatServerTimeToLocalTime(serverTime: String): String {
        return try {
            // 서버 시간 형식 (ISO 8601 기준 예제)
            val serverFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())

            // 서버 시간 파싱
            val date = serverFormatter.parse(serverTime)

            // 로컬 시간 형식
            val localFormatter = SimpleDateFormat("a hh:mm", Locale.getDefault()) // "오후 04:47" 형식
            localFormatter.timeZone = TimeZone.getDefault() // 로컬 시간대 적용

            // 변환된 시간 반환
            localFormatter.format(date!!)
        } catch (e: Exception) {
            // 파싱 실패 시 서버 시간 그대로 반환
            serverTime
        }
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
