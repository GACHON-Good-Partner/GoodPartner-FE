package com.example.goodpartner.ui.dashboard

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
    val id: Long, // 메시지 ID
    val userId: String?, // 사용자 ID
    val message: String, // 메시지 내용
    val status: String?, // 상태 (REQUEST 또는 RESPONSE)
    val createdAt: String?, // 생성 시간
    val updatedAt: String? // 수정 시간
)

// 누적 질문 수 응답
data class QuestionCountResponse(
    val code: Int,
    val message: String,
    val data: Int // 누적 질문 수
)
