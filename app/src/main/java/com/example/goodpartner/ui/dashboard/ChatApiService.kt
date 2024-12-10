package com.example.goodpartner.ui.dashboard

import retrofit2.Call
import retrofit2.http.*

interface ChatApiService {
    @GET("/chats")
    fun getChatHistory(
        @Header("Authorization") token: String
    ): Call<ChatHistoryResponse> // 응답 형식을 ChatHistoryResponse로 변경

    // 메시지 전송 (Request 사용)
    @POST("/chats")
    @FormUrlEncoded
    fun postChatMessage(
        @Header("Authorization") token: String,
        @Field("message") message: String // 쿼리 파라미터로 전송
    ): Call<ChatApiResponse>

    // 기존 채팅 내역 조회
    @GET("/chats/latest")
    fun getLatestChats(
        @Header("Authorization") token: String // Authorization 헤더로 JWT 토큰 전달
    ): Call<ChatHistoryResponse>

    // 누적 질문 수 조회
    @GET("/chats/count")
    fun getQuestionCount(
        @Header("Authorization") token: String // Authorization 헤더로 JWT 토큰 전달
    ): Call<QuestionCountResponse>

}
