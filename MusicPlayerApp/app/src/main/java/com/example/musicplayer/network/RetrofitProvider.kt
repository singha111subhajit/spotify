package com.example.musicplayer.network

import android.content.Context
import com.example.musicplayer.config.ApiConfig
import com.example.musicplayer.network.api.AuthApi
import com.example.musicplayer.network.api.MusicApi
import com.example.musicplayer.storage.TokenStorage
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    @Volatile private var retrofit: Retrofit? = null

    fun getRetrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val tokenStorage = TokenStorage.getInstance(context)
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStorage))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder().create()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun getAuthApi(context: Context): AuthApi = getRetrofit(context).create(AuthApi::class.java)
    fun getMusicApi(context: Context): MusicApi = getRetrofit(context).create(MusicApi::class.java)
}