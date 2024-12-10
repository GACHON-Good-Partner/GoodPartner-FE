package com.example.goodpartner.ui.MyPage

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.goodpartner.R
import com.example.goodpartner.databinding.FragmentMypageUpdateBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MyPageUDFragment : Fragment() {

    private var _binding: FragmentMypageUpdateBinding? = null
    private val binding get() = _binding!!

    // 기존 사용자 정보 저장
    private var originalName: String = ""
    private var originalEmail: String = ""
    private var originalPhone: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View 바인딩 초기화
        _binding = FragmentMypageUpdateBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 기존 사용자 정보를 가져와서 UI에 표시
        fetchOriginalUserInfo()

        // 수정 버튼 클릭 이벤트
        binding.btnProfileChange2.setOnClickListener {
            updateUserProfile()
        }

        // 뒤로가기 버튼 설정
        binding.mypageUpdateBack.setOnClickListener {
            // 네비게이션 바 표시
            activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.visibility = View.VISIBLE
            activity?.onBackPressed()
        }

        return root
    }

    /**
     * 기존 사용자 정보를 가져와서 입력 필드에 표시
     */
    private fun fetchOriginalUserInfo() {
        val accessToken = requireContext()
            .getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            .getString("accessToken", null)

        if (accessToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://13.209.81.42.nip.io/users" // 사용자 정보 조회 엔드포인트
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
                Log.e("MyPageUDFragment", "서버 요청 실패: ${e.localizedMessage}")
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "서버 요청에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val data = jsonObject.optJSONObject("data")
                        originalName = data?.optString("name", "") ?: ""
                        originalEmail = data?.optString("email", "") ?: ""
                        originalPhone = data?.optString("tel", "") ?: ""

                        requireActivity().runOnUiThread {
                            binding.nameInput.setText(originalName)
                            binding.emailInput.setText(originalEmail)
                            binding.phoneInput.setText(originalPhone)
                        }
                    } catch (e: Exception) {
                        Log.e("MyPageUDFragment", "응답 파싱 실패: ${e.localizedMessage}")
                    }
                } else {
                    Log.e("MyPageUDFragment", "서버 응답 실패: ${response.code}")
                }
            }
        })
    }

    /**
     * 사용자 프로필 수정 요청
     */
    private fun updateUserProfile() {
        val accessToken = requireContext()
            .getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            .getString("accessToken", null)

        if (accessToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedName = binding.nameInput.text.toString()
        val updatedEmail = binding.emailInput.text.toString()
        val updatedPhone = binding.phoneInput.text.toString()

        val requestBody = JSONObject().apply {
            // 값이 입력되지 않은 경우 기존 값 사용
            put("name", if (updatedName.isEmpty()) originalName else updatedName)
            put("email", if (updatedEmail.isEmpty()) originalEmail else updatedEmail)
            put("tel", if (updatedPhone.isEmpty()) originalPhone else updatedPhone)
        }.toString().toRequestBody("application/json".toMediaType())

        val url = "https://13.209.81.42.nip.io/users" // 사용자 정보 수정 엔드포인트
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .patch(requestBody) // PATCH 요청
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MyPageUDFragment", "서버 요청 실패: ${e.localizedMessage}")
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "프로필 수정 요청에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val message = jsonObject.optString("message", "프로필 수정 성공")
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            Log.i("MyPageUDFragment", message)
                            // 수정 성공 후 MyPageFragment로 이동
                            findNavController().navigateUp()
                        }
                    } catch (e: Exception) {
                        Log.e("MyPageUDFragment", "응답 처리 실패: ${e.localizedMessage}")
                    }
                } else {
                    Log.e("MyPageUDFragment", "서버 응답 실패: ${response.code}")
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
