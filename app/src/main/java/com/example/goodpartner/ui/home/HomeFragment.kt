package com.example.goodpartner.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.goodpartner.databinding.FragmentHomeBinding
import com.example.goodpartner.ui.dashboard.ChatApiService
import com.example.goodpartner.ui.dashboard.QuestionCountResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatApi: ChatApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Retrofit 초기화
        setupRetrofit()

        // 누적 질문 수 조회
        loadQuestionCount()

        return root
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://13.209.81.42.nip.io/") // 서버 URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        chatApi = retrofit.create(ChatApiService::class.java)
    }

    private fun loadQuestionCount() {
        val token = getAccessToken() ?: return
        chatApi.getQuestionCount("Bearer $token").enqueue(object : Callback<QuestionCountResponse> {
            override fun onResponse(
                call: Call<QuestionCountResponse>,
                response: Response<QuestionCountResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { questionCount ->
                        updateQuestionCountUI(questionCount.data)
                    }
                } else {
                    Log.e("HomeFragment", "누적 질문 수 조회 실패: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<QuestionCountResponse>, t: Throwable) {
                Log.e("HomeFragment", "누적 질문 수 조회 중 오류 발생", t)
            }
        })
    }

    private fun updateQuestionCountUI(count: Int) {
        binding.questionCountTextView.text = "누적 질문 수: $count"
    }

    private fun getAccessToken(): String? {
        val sharedPreferences =
            requireActivity().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getString("accessToken", null)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}