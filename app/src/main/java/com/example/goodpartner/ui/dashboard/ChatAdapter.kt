package com.example.goodpartner.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.goodpartner.R

// ChatItem 클래스
data class ChatItem(
    val message: String,
    val sender: String,
    val time: String,
    val keywords: List<KeywordResponse>? = null // 키워드 응답
)

// ChatAdapter 클래스
class ChatAdapter(
    private val chatList: List<ChatItem>,
    private val formatDateTime: (String) -> String // 시간 변환 함수 전달받음
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatItem = chatList[position]
        if (holder is ChatViewHolder) {
            if (chatItem.sender == "user") {
                // 사용자 메시지 처리
                holder.userMessageLayout.visibility = View.VISIBLE
                holder.serverMessageLayout.visibility = View.GONE
                holder.userChatMessage.text = chatItem.message
                holder.userChatTime.text = formatDateTime(chatItem.time) // 시간 변환
                holder.serverKeywordsLayout.visibility = View.GONE // 키워드는 서버 메시지에서만 표시
            } else {
                // 서버 메시지 처리
                holder.serverMessageLayout.visibility = View.VISIBLE
                holder.userMessageLayout.visibility = View.GONE
                holder.serverChatMessage.text = chatItem.message
                holder.serverChatTime.text = formatDateTime(chatItem.time) // 시간 변환

                // 키워드가 있을 경우 키워드 UI 업데이트
                if (!chatItem.keywords.isNullOrEmpty()) {
                    holder.serverKeywordsLayout.visibility = View.VISIBLE
                    holder.serverKeywordsLayout.removeAllViews()

                    // 각 키워드를 동적으로 추가
                    chatItem.keywords.forEach { keyword ->
                        val keywordView = TextView(holder.itemView.context).apply {
                            text = keyword.keyWord
                            setPadding(8, 8, 8, 8)
                            setTextColor(holder.itemView.context.getColor(R.color.blue)) // 키워드 색상
                            setOnClickListener {
                                // 키워드를 클릭하면 브라우저로 이동
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(keyword.url))
                                holder.itemView.context.startActivity(intent)
                            }
                        }
                        holder.serverKeywordsLayout.addView(keywordView)
                    }
                } else {
                    holder.serverKeywordsLayout.visibility = View.GONE // 키워드가 없으면 숨김
                }
            }
        }
    }

    override fun getItemCount(): Int = chatList.size

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serverMessageLayout: View = view.findViewById(R.id.server_message_layout)
        val userMessageLayout: View = view.findViewById(R.id.user_message_layout)

        val serverChatMessage: TextView = view.findViewById(R.id.chat_message)
        val serverChatTime: TextView = view.findViewById(R.id.chat_time)
        val serverKeywordsLayout: LinearLayout = view.findViewById(R.id.server_keywords_layout) // 동적 키워드 레이아웃

        val userChatMessage: TextView = view.findViewById(R.id.user_chat_message)
        val userChatTime: TextView = view.findViewById(R.id.user_chat_time)
    }
}
