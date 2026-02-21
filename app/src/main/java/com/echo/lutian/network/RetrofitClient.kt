package com.echo.lutian.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 客户端单例 - 支持动态配置 Base URL
 */
object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var currentBaseUrl: String = ""
    private var retrofit: Retrofit? = null
    var apiService: ApiService? = null
        private set

    fun init(baseUrl: String) {
        if (baseUrl.isBlank()) {
            apiService = null
            currentBaseUrl = ""
            return
        }
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (currentBaseUrl != safeUrl || apiService == null) {
            currentBaseUrl = safeUrl
            try {
                retrofit = Retrofit.Builder()
                    .baseUrl(safeUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                apiService = retrofit?.create(ApiService::class.java)
            } catch (e: Exception) {
                apiService = null
            }
        }
    }
}
