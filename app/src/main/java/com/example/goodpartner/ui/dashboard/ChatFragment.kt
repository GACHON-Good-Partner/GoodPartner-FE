package com.example.goodpartner.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.goodpartner.databinding.FragmentChatBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val chatViewModel =
            ViewModelProvider(this).get(ChatViewModel::class.java)

        _binding = FragmentChatBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 네비게이션바 숨기기
        activity?.findViewById<BottomNavigationView>(com.example.goodpartner.R.id.nav_view)?.visibility = View.GONE

        // ViewModel 관찰
        val textView: TextView = binding.chatTitle
        chatViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // 뒤로가기 버튼 클릭 리스너
        binding.chatBackButton.setOnClickListener {
            activity?.onBackPressed() // 뒤로가기
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 네비게이션바 다시 보이게 설정
        activity?.findViewById<BottomNavigationView>(com.example.goodpartner.R.id.nav_view)?.visibility = View.VISIBLE

        _binding = null
    }
}
