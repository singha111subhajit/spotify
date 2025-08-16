package com.example.musicplayer.repository

import android.content.Context
import com.example.musicplayer.model.AuthResponse
import com.example.musicplayer.model.LoginRequest
import com.example.musicplayer.model.RegisterRequest
import com.example.musicplayer.network.RetrofitProvider
import com.example.musicplayer.storage.TokenStorage

class AuthRepository(context: Context) {
    private val authApi = RetrofitProvider.getAuthApi(context)
    private val tokenStorage = TokenStorage.getInstance(context)

    suspend fun login(email: String, password: String): AuthResponse {
        val response = authApi.login(LoginRequest(email, password))
        tokenStorage.setToken(response.token)
        return response
    }

    suspend fun register(email: String, password: String): AuthResponse {
        val response = authApi.register(RegisterRequest(email, password))
        tokenStorage.setToken(response.token)
        return response
    }

    fun logout() {
        tokenStorage.clear()
    }

    fun isLoggedIn(): Boolean = !tokenStorage.getToken().isNullOrBlank()
}