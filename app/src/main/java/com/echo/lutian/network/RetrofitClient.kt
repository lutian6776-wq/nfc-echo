package com.echo.lutian.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 客户端单例
 */
object RetrofitClient {

    // 配置你的后端 URL
    // 本地测试：http://10.0.2.2:3000/ (Android 模拟器访问本机)
    // 生产环境：https://your-app.cloud.sealos.io/
    private const val BASE_URL = "https://efbasmrcgcxb.sealoshzh.site/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(10, TimeUnit.SECONDS)  // 优化：从 30 秒减少到 10 秒
        .readTimeout(15, TimeUnit.SECONDS)     // 优化：从 30 秒减少到 15 秒
        .writeTimeout(15, TimeUnit.SECONDS)    // 优化：从 30 秒减少到 15 秒
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
