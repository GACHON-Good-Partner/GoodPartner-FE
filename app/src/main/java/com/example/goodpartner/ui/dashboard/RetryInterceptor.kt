package com.example.goodpartner.ui.dashboard

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor(private val maxRetries: Int, private val retryDelayMillis: Long) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var response: Response? = null
        var lastException: IOException? = null

        while (attempt < maxRetries) {
            try {
                response = chain.proceed(chain.request())
                if (response.isSuccessful) {
                    return response
                }
            } catch (e: IOException) {
                lastException = e
            }

            attempt++
            Thread.sleep(retryDelayMillis * attempt) // 지수 백오프
        }

        // 모든 시도가 실패하면 마지막 예외를 던짐
        throw lastException ?: IOException("Unknown error during request retries")
    }
}
