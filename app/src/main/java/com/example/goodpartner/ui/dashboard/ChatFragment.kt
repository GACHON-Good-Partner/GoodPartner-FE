package com.example.goodpartner.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils.formatDateTime
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
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
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

    private var lastMessageId: Long? = null // 마지막 메시지 ID 저장

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

        // 새로운 채팅 세션 시작
        startNewChatSession()

        // 메시지 전송 버튼 초기화
        setupSendButton()

        return root
    }

    /**
     * RecyclerView 초기화
     */
    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatList, :: formatDateTime)
        binding.chatRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }



    /**
     * Retrofit 초기화
     */
    private fun setupRetrofit() {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(RetryInterceptor(3, 1000)) // 재시도 인터셉터 추가
            .build()

        val gson = GsonBuilder()
            .serializeNulls()
            .setLenient() // 서버의 약간 불완전한 JSON 처리 허용
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://13.209.81.42.nip.io/") // 서버 URL
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        chatApi = retrofit.create(ChatApiService::class.java)
    }

    /**
     * 새로운 채팅 세션 시작
     */
    private fun startNewChatSession() {
        // 채팅 목록 초기화
        chatList.clear()
        chatAdapter.notifyDataSetChanged()

        // 새로운 메시지 ID를 저장하지 않음 (새로운 세션)
        lastMessageId = null

        Log.d("ChatFragment", "새로운 채팅 세션이 시작되었습니다.")
    }

    /**
     * 메시지 전송 버튼 클릭 리스너
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
    private fun sendMessage(message: String, retryCount: Int = 0) {
        val token = getAccessToken() ?: return

        // 요청 데이터와 토큰 확인 로그
        Log.d("ChatFragment", "전송할 메시지: $message")
        Log.d("ChatFragment", "Authorization 토큰: Bearer $token")

        val request = ChatRequest(message)
        chatApi.postChatMessage("Bearer $token", message).enqueue(object : Callback<ChatApiResponse> {
            override fun onResponse(call: Call<ChatApiResponse>, response: Response<ChatApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.data != null) {
                        val chatResponse = apiResponse.data
                        Log.d("ChatFragment", "서버 응답 성공: $chatResponse")

                        // 새로운 메시지를 UI에 추가
                        val userChat = ChatItem(message, "user", chatResponse.createdAt ?: "시간 없음")
                        val serverChat = ChatItem(chatResponse.message, "server", chatResponse.createdAt ?: "시간 없음")

                        // 최신 메시지 ID 갱신
                        lastMessageId = chatResponse.id
                        Log.d("ChatFragment", "최신 메시지 ID 갱신: $lastMessageId")
                        Log.d("ChatFragment", "서버 응답 본문: ${response.body()}")

                        updateChatUi(userChat, serverChat)
                    } else {
                        Log.e("ChatFragment", "서버 응답 성공: 하지만 데이터가 없습니다.")
                    }
                }
                else if (response.code() == 401) {
                    // 토큰이 유효하지 않을 경우
                    Log.e("ChatFragment", "토큰이 유효하지 않습니다. 자동 로그아웃 처리.")
                    handleInvalidToken() // 로그아웃 처리
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ChatFragment", "메시지 전송 실패: 코드=${response.code()}, 메시지=${response.message()}, 에러 본문=$errorBody")
                }
            }

            override fun onFailure(call: Call<ChatApiResponse>, t: Throwable) {
                if (retryCount < 3) {
                    Log.w("ChatFragment", "메시지 전송 실패, 재시도: ${retryCount + 1}회")
                    sendMessage(message, retryCount + 1) // 재귀 호출로 재시도
                } else {
                    Log.e("ChatFragment", "메시지 전송 실패, 재시도 횟수 초과", t)
                    Toast.makeText(requireContext(), "메시지 전송에 실패했습니다. 네트워크 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /**
     * 유효하지 않은 토큰 처리: SharedPreferences에서 제거 후 LoginActivity로 이동
     */
    private fun handleInvalidToken() {
        Toast.makeText(requireContext(), "토큰이 유효하지 않습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()

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
            inputFormat.timeZone = TimeZone.getTimeZone("UTC-9:00") // 서버 시간이 UTC라면 설정

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

    /**
     * Fragment 종료 시 View Binding 해제
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
