package com.example.goodpartner.Login

import android.app.Application
import com.example.goodpartner.R
import com.kakao.sdk.common.KakaoSdk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, getString(R.string.kakao_app_key))
    }
}
