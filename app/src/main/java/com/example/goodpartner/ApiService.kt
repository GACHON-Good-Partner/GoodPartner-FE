package com.example.goodpartner

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // 1. 회원 정보 조회 (GET /users)
    @GET("/users")
    fun getUserInfo(
        @Header("Authorization") token: String
    ): Call<ResponseBody>

    // 2. 회원 정보 수정 (PATCH /users)
    @PATCH("/users")
    fun updateUserInfo(
        @Header("Authorization") token: String,
        @Body updatedInfo: Map<String, Any>
    ): Call<ResponseBody>

    // 3. 사용자 채팅 내역 조회 (GET /chats)
    @GET("/chats")
    fun getChatHistory(
        @Header("Authorization") token: String
    ): Call<ResponseBody>

    // 4. 질문하기 (POST /chats)
    @POST("/chats")
    fun askQuestion(
        @Header("Authorization") token: String,
        @Body question: Map<String, Any>
    ): Call<ResponseBody>

    // 5. 누적 질문 수 조회 (GET /chats/count)
    @GET("/chats/count")
    fun getTotalQuestionCount(
        @Header("Authorization") token: String
    ): Call<ResponseBody>

    // 6. 최근 질문 3개 조회 (GET /chats/latest)
    @GET("/chats/latest")
    fun getLatestQuestions(
        @Header("Authorization") token: String
    ): Call<ResponseBody>
}
