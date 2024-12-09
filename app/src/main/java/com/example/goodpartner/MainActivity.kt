package com.example.goodpartner

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.goodpartner.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kakao.sdk.common.util.Utility

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 액션바 숨기기
        supportActionBar?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        // NavController 연결
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // BottomNavigationView와 NavController 연결
        navView.setupWithNavController(navController)

        var keyHash = Utility.getKeyHash(this)
        Log.i("kjwTest", "keyHash: $keyHash")
    }


}