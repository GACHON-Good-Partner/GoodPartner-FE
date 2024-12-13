package com.example.goodpartner.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils.formatDateTime
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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

    private var currentChatId: Long? = null // 현재 질문에 대한 chatId

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 네비게이션바 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.GONE

        // 토큰 유효성 검사
        validateToken()

        // 뒤로가기 버튼 설정
        binding.chatBackButton.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.VISIBLE
            activity?.onBackPressed()
        }

        // RecyclerView 초기화
        setupRecyclerView()

        // Retrofit 초기화
        setupRetrofit()

        // 기존 채팅 내역 로드
        loadChatHistory()

        // 메시지 전송 버튼 초기화
        setupSendButton()

        val chatId = arguments?.getLong("chatId", -1) ?: -1
        if (chatId != -1L) {
            Log.d("ChatFragment", "전달받은 chatId: $chatId")
            loadChatById(chatId) // 특정 chatId에 해당하는 데이터를 로드
        }

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
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // 서버 연결 타임아웃
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)    // 응답 읽기 타임아웃
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)   // 요청 쓰기 타임아웃
            .build()

        val gson = GsonBuilder().serializeNulls().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://13.209.81.42.nip.io/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        chatApi = retrofit.create(ChatApiService::class.java)
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

        // 간단한 유효성 확인 (예: 토큰 길이 또는 서버 호출)
        if (token.length < 20) { // 기본적인 길이 확인
            Log.e("ChatFragment", "토큰이 유효하지 않습니다.")
            handleInvalidToken()
        } else {
            Log.d("ChatFragment", "토큰이 유효합니다.")
        }
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
     * 채팅 내역 불러오기
     */
    private fun loadChatHistory() {
        val token = getAccessToken() ?: return

        chatApi.getChatHistory("Bearer $token").enqueue(object : Callback<ChatHistoryResponse> {
            override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                if (response.isSuccessful) {
                    response.body()?.data?.let { history ->
                        chatList.clear()

                        // 초기 환영 메시지를 추가
                        addInitialMessage()

                        // 서버로부터 받은 채팅 내역 추가
                        history.forEach { chat ->
                            val sender = if (chat.status == "REQUEST") "user" else "server"
                            chatList.add(
                                ChatItem(
                                    message = chat.message,
                                    sender = sender,
                                    time = chat.createdAt ?: "시간 없음",
                                    keywords = chat.keywordResponses
                                )
                            )
                        }
                        chatAdapter.notifyDataSetChanged()

                        // 마지막 메시지로 스크롤
                        binding.chatRecyclerView.post {
                            binding.chatRecyclerView.scrollToPosition(chatList.size - 1)
                        }
                    }
                } else {
                    Log.e("ChatFragment", "채팅 내역 로드 실패: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {
                Log.e("ChatFragment", "채팅 내역 로드 중 오류 발생", t)
            }
        })
    }

    /**
     * 초기 환영 메시지 추가
     */
    private fun addInitialMessage() {
        val initialMessage = ChatItem(
            message = "굿파트너를 이용해 주시는 의뢰인님,\n\n법률 안내를 도와드리는 챗봇 버비입니다.\n\n원하시는 법률 질문을 해 주시면 의뢰인님의 질문에 답변드리겠습니다.\n\n구체적으로 작성해 주실수록 더욱 의뢰인님께 적절한 법률 안내를 해 드릴 수 있다는 점 참고 부탁드립니다.",
            sender = "server",
            time = SimpleDateFormat("a hh:mm", Locale.getDefault()).format(Date()),
            keywords = null
        )

        // 초기 메시지를 채팅 리스트의 상단에 추가
        chatList.add(0, initialMessage)
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
    private fun sendMessage(message: String) {
        val token = getAccessToken() ?: return

        // 사용자 메시지 추가
        val userChat = ChatItem(
            message = message,
            sender = "user",
            time = SimpleDateFormat("a hh:mm", Locale.getDefault()).format(Date())
        )
        chatList.add(userChat)
        chatAdapter.notifyItemInserted(chatList.size - 1)
        binding.chatRecyclerView.scrollToPosition(chatList.size - 1)

        // 로딩 메시지 추가
        val loadingPosition = chatList.size
        val loadingChat = ChatItem(
            message = "...",
            sender = "server",
            time = SimpleDateFormat("a hh:mm", Locale.getDefault()).format(Date())
        )
        chatList.add(loadingChat)
        chatAdapter.notifyItemInserted(loadingPosition)
        binding.chatRecyclerView.scrollToPosition(loadingPosition)

        val request = ChatRequest(message)
        chatApi.postChatMessage("Bearer $token", request).enqueue(object : Callback<ChatApiResponse> {
            override fun onResponse(call: Call<ChatApiResponse>, response: Response<ChatApiResponse>) {

                // 로딩 메시지 제거
                chatList.removeAt(loadingPosition)
                chatAdapter.notifyItemRemoved(loadingPosition)

                if (response.isSuccessful) {
                    val chatResponse = response.body()?.data
                    Log.d("ChatFragment", "서버 응답 성공: $chatResponse")
                    if (chatResponse != null) {
                        currentChatId = chatResponse.chatId // 새로운 chatId 저장
                        Log.d("ChatFragment", "새로운 chatId: $currentChatId")

                        val userChat = ChatItem(
                            message = message,
                            sender = "user",
                            time = chatResponse.createdAt ?: "시간 없음",
                            keywords = null
                        )
                        val serverChat = ChatItem(
                            message = chatResponse.message,
                            sender = "server",
                            time = chatResponse.createdAt ?: "시간 없음",
                            keywords = chatResponse.keywordResponses
                        )

                        updateChatUi(userChat, serverChat)

                        // 채팅입력후 edittext 내용 지우기
                        binding.messageInput.text.clear()

                        // 키보드 닫기
                        hideKeyboard()
                    }
                }  else if (response.code() == 401) {
                    // 401 응답: 토큰이 유효하지 않은 경우 처리
                    Log.e("ChatFragment", "토큰이 유효하지 않습니다. 자동 로그아웃 처리.")
                    handleInvalidToken()
                } else {
                    Log.e("ChatFragment", "메시지 전송 실패: 코드=${response.code()}, 메시지=${response.message()}")
                }
            }

            override fun onFailure(call: Call<ChatApiResponse>, t: Throwable) {
                // 로딩 메시지 제거
                chatList.removeAt(loadingPosition)
                chatAdapter.notifyItemRemoved(loadingPosition)

                Log.e("ChatFragment", "메시지 전송 중 오류 발생", t)
            }
        })
    }

    private fun formatDateTime(isoDateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

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
     * UI 업데이트: 채팅 목록에 사용자와 서버 메시지 추가
     */
    private fun updateChatUi(userChat: ChatItem, serverChat: ChatItem) {
        chatList.add(userChat)
        chatList.add(serverChat)
        chatAdapter.notifyItemRangeInserted(chatList.size - 2, 2)
        binding.chatRecyclerView.scrollToPosition(chatList.size - 1)
    }

    /**
     * 채팅 보낸 후 키보드 창 내리기
     */
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.messageInput.windowToken, 0)
    }


    /**
     * SharedPreferences에서 Access Token 가져오기
     */
    private fun getAccessToken(): String? {
        val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getString("accessToken", null)
    }


    /**
     * 최근 질문 3개 조회후 채팅방 이동
     */
    private fun loadChatById(chatId: Long) {
        val token = getAccessToken() ?: return

        chatApi.getChatHistory("Bearer $token").enqueue(object : Callback<ChatHistoryResponse> {
            override fun onResponse(call: Call<ChatHistoryResponse>, response: Response<ChatHistoryResponse>) {
                if (response.isSuccessful) {
                    response.body()?.data?.let { history ->
                        val targetChat = history.find { it.chatId == chatId }
                        if (targetChat != null) {
                            // 채팅 UI 업데이트
                            updateChatUi(targetChat)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ChatHistoryResponse>, t: Throwable) {
                Log.e("ChatFragment", "채팅 데이터 로드 실패", t)
            }
        })
    }

    private fun updateChatUi(chat: ChatResponse) {
        // 선택한 chatId에 해당하는 메시지를 UI에 표시
        chatList.add(
            ChatItem(
                message = chat.message,
                sender = if (chat.status == "REQUEST") "user" else "server",
                time = chat.createdAt ?: "시간 없음",
                keywords = chat.keywordResponses
            )
        )
        chatAdapter.notifyDataSetChanged()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
