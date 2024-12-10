package com.example.goodpartner.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.goodpartner.R

// ChatItem 클래스
data class ChatItem(val message: String, val sender: String, val time: String) // sender: "user" or "server"

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
            } else {
                // 서버 메시지 처리
                holder.serverMessageLayout.visibility = View.VISIBLE
                holder.userMessageLayout.visibility = View.GONE
                holder.serverChatMessage.text = chatItem.message
                holder.serverChatTime.text = formatDateTime(chatItem.time) // 시간 변환
            }
        }
    }

    override fun getItemCount(): Int = chatList.size

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val serverMessageLayout: View = view.findViewById(R.id.server_message_layout)
        val userMessageLayout: View = view.findViewById(R.id.user_message_layout)

        val serverChatMessage: TextView = view.findViewById(R.id.chat_message)
        val serverChatTime: TextView = view.findViewById(R.id.chat_time)

        val userChatMessage: TextView = view.findViewById(R.id.user_chat_message)
        val userChatTime: TextView = view.findViewById(R.id.user_chat_time)
    }
}
