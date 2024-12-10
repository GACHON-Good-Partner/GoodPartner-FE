package com.example.goodpartner.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.goodpartner.Login.LoginActivity
import com.example.goodpartner.R
import com.example.goodpartner.databinding.FragmentChatBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val chatList = mutableListOf<ChatItem>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatApi: ChatApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 네비게이션바 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.GONE

        // 뒤로가기 버튼 설정
        binding.chatBackButton.setOnClickListener {
            // 네비게이션 바 표시
            activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.VISIBLE
            activity?.onBackPressed()
        }


        // RecyclerView 초기화
        setupRecyclerView()

        // Retrofit 초기화
        setupRetrofit()

        // 채팅 내역 불러오기
        loadChatHistory()

        // 메시지 전송 버튼 초기화
        setupSendButton()

        // 토큰 유효성 검사
        validateToken()


        return root
    }

    /**
     * 토큰 유효성 검사: 토큰이 없거나 유효하지 않을 경우 로그인 화면으로 이동
     */
    private fun validateToken() {
        val token = getAccessToken()
        if (token.isNullOrEmpty()) {
            Log.e("ChatFragment", "토큰이 없습니다.")
            handleInvalidToken()
            return
        }

        Log.d("ChatFragment", "유효한 토큰 확인: Bearer $token")

        // 간단한 유효성 확인 (예: 토큰 길이)
        if (token.length < 20) {
            Log.e("ChatFragment", "토큰이 유효하지 않습니다.")
            handleInvalidToken()
        } else {
            Log.d("ChatFragment", "토큰이 유효합니다. API 호출 시작.")
        }
    }

    /**
     * 유효하지 않은 토큰 처리: SharedPreferences에서 제거 후 LoginActivity로 이동
     */
    private fun handleInvalidToken() {
        Toast.makeText(requireContext(), "토큰이 유효하지 않습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
        logoutAndRedirectToLogin()
    }

    /**
     * 로그아웃 처리 및 로그인 화면으로 이동
     */
    private fun logoutAndRedirectToLogin() {
        // SharedPreferences에서 토큰 제거
        val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("accessToken")
        editor.apply()

        // LoginActivity로 이동
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    /**
     * RecyclerView 초기화: ChatAdapter를 연결
     */
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatList, :: formatDateTime)
        binding.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    /**
     * Retrofit 초기화: ChatApiService 연결
     */
    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://13.209.81.42.nip.io/") // 서버 URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        chatApi = retrofit.create(ChatApiService::class.java)
    }

    /**
     * 서버에서 채팅 내역 가져오기
     */
    private fun loadChatHistory() {
        val token = getAccessToken() ?: return
        chatApi.getChatHistory("Bearer $token").enqueue(object : Callback<ChatHistoryResponse> {
            override fun onResponse(
                call: Call<ChatHistoryResponse>,
                response: Response<ChatHistoryResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { chatHistory ->
                        Log.d("ChatFragment", "채팅 내역 로드 성공: $chatHistory")
                        chatList.clear()
                        chatHistory.data.forEach { chat ->
                            val sender = if (chat.status == "REQUEST") "user" else "server"
                            val time = chat.createdAt ?: "시간 없음" // null 처리
                            chatList.add(ChatItem(chat.message, sender, time))
                        }
                        chatAdapter.notifyDataSetChanged()
                    }
                } else if (response.code() == 401) {
                    Log.e("ChatFragment", "토큰이 유효하지 않습니다. (401 Unauthorized)")
                    handleInvalidToken()
                } else {
                    Log.e("ChatFragment", "채팅 내역 로드 실패: 코드=${response.code()}, 메시지=${response.message()}")
                }
            }

            override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {
                Log.e("ChatFragment", "채팅 내역 로드 중 오류 발생", t)
            }
        })
    }

    // 최근 질문 3개 조회
    private fun loadLatestChats() {
        val token = getAccessToken() ?: return
        chatApi.getLatestChats("Bearer $token").enqueue(object : Callback<ChatHistoryResponse> {
            override fun onResponse(
                call: Call<ChatHistoryResponse>,
                response: Response<ChatHistoryResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { latestChats ->
                        latestChats.data.forEach { chat ->
                            val sender = if (chat.status == "REQUEST") "user" else "server"
                            val time = chat.createdAt ?: "시간 없음" // null 처리
                            chatList.add(ChatItem(chat.message, sender, time))
                        }
                        chatAdapter.notifyDataSetChanged()
                    }
                } else {
                    Log.e("ChatFragment", "최근 질문 3개 조회 실패: ${response.errorBody()}")
                }
            }

            override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {
                Log.e("ChatFragment", "최근 질문 3개 조회 중 오류 발생", t)
            }
        })
    }

    /**
     * 메시지 전송 버튼 설정
     */
    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString().trim()
            if (message.isEmpty()) {
                Toast.makeText(requireContext(), "메시지를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendMessage(message)
        }
    }

    /**
     * 서버로 메시지 전송
     */
    private fun sendMessage(message: String) {
        val token = getAccessToken() ?: return

        // 요청 데이터와 토큰 확인 로그
        Log.d("ChatFragment", "전송할 메시지: $message")
        Log.d("ChatFragment", "Authorization 토큰: Bearer $token")

        val request = ChatRequest(message)
        Log.d("ChatFragment", "전송할 요청 데이터: $request")

        chatApi.postChatMessage("Bearer $token", message).enqueue(object : Callback<ChatApiResponse> {
            override fun onResponse(call: Call<ChatApiResponse>, response: Response<ChatApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.data != null) {
                        val chatResponse = apiResponse.data
                        Log.d("ChatFragment", "서버 응답 성공: $chatResponse")

                        val userChat = ChatItem(message, "user", chatResponse.createdAt ?: "시간 없음")
                        val serverChat = ChatItem(chatResponse.message, "server", chatResponse.createdAt ?: "시간 없음")
                        updateChatUi(userChat, serverChat)
                    } else {
                        Log.e("ChatFragment", "서버 응답 성공: 하지만 데이터가 없습니다.")
                    }
                } else {
                    Log.e("ChatFragment", "메시지 전송 실패: 코드=${response.code()}, 메시지=${response.message()}")
                    Log.e("ChatFragment", "서버 응답 본문: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ChatApiResponse>, t: Throwable) {
                Log.e("ChatFragment", "메시지 전송 중 오류 발생", t)
            }
        })
    }



    /**
     * UI 업데이트: 채팅 목록에 사용자와 서버 메시지 추가
     */
    private fun updateChatUi(userChat: ChatItem, serverChat: ChatItem) {
        chatList.add(userChat)
        chatList.add(serverChat)
        chatAdapter.notifyItemRangeInserted(chatList.size - 2, 2)
        binding.chatRecyclerView.scrollToPosition(chatList.size - 1)
    }

    /**
     * 채팅 현재 시간 구현
     */
    private fun formatDateTime(isoDateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("GMT-3:00") // 서버 시간이 UTC라면 설정

            val date = inputFormat.parse(isoDateTime)

            // 출력 포맷 설정 (오전/오후 포함)
            val outputFormat = SimpleDateFormat("a hh:mm", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault() // 로컬 시간대로 설정
            outputFormat.format(date!!)
        } catch (e: Exception) {
            isoDateTime // 실패 시 원본 반환
        }
    }


    /**
     * SharedPreferences에서 Access Token 가져오기
     */
    private fun getAccessToken(): String? {
        val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getString("accessToken", null)
    }

    override fun onResume() {
        super.onResume()

        // 기존 데이터 유지 및 새로운 데이터 추가
        loadLatestChats()
    }


    /**
     * Fragment 종료 시 View Binding 해제
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
}
