package com.example.goodpartner.ui.MyPage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.goodpartner.R
import com.example.goodpartner.ui.dashboard.ChatResponse
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class RecentQuestionsAdapter(
    private val questions: List<ChatResponse>,
    private val onItemClick: (ChatResponse) -> Unit // 클릭 이벤트 리스너 추가
) : RecyclerView.Adapter<RecentQuestionsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val questionTextView: TextView = view.findViewById(R.id.questionTextView)
        val createdAtTextView: TextView = view.findViewById(R.id.createdAtTextView)

        fun bind(chatResponse: ChatResponse) {
            questionTextView.text = chatResponse.message
            createdAtTextView.text = formatDateTime(chatResponse.createdAt ?: "시간 없음")

            // 클릭 리스너 연결
            itemView.setOnClickListener { onItemClick(chatResponse) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recent_question_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(questions[position])
    }

    override fun getItemCount(): Int = questions.size

    private fun formatDateTime(isoDateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val date = inputFormat.parse(isoDateTime)

            val outputFormat = SimpleDateFormat("a hh:mm", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()
            outputFormat.format(date!!)
        } catch (e: Exception) {
            isoDateTime
        }
    }
}
