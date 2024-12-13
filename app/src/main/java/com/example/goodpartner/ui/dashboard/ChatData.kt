package com.example.goodpartner.ui.dashboard

import com.google.gson.annotations.SerializedName

// 메시지 전송 요청
data class ChatRequest(
    val message: String // 메시지 내용
)

// 채팅 내역 응답
data class ChatHistoryResponse(
    val code: Int, // 응답 코드
    val message: String, // 응답 메시지
    val data: List<ChatResponse> // 실제 데이터는 여기에 포함됨
)

// 단일 메시지 응답
data class ChatApiResponse(
    val code: Int, // 응답 코드
    val message: String, // 응답 메시지
    val data: ChatResponse? // 서버의 data 필드를 매핑
)

// 메시지 데이터 구조
data class ChatResponse(
    val chatId: Long,
    val userId: String,
    val message: String,
    val status: String,
    val createdAt: String?,
    val updatedAt: String?,
    val keywordResponses: List<KeywordResponse>? // 키워드 리스트 추가
)

data class KeywordResponse(
    val keyWord: String, // 키워드 이름
    val url: String // 키워드에 연결된 URL
)



// 누적 질문 수 응답
data class QuestionCountResponse(
    val code: Int,
    val message: String,
    val data: Int // 누적 질문 수
)

