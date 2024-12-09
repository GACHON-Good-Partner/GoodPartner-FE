package com.example.goodpartner.ui.MyPage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MyPageViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "사용자 정보"
    }
    val text: LiveData<String> = _text
}