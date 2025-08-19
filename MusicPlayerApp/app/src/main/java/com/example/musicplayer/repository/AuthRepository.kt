package com.example.DhoonHub.repository

import android.content.Context
import com.example.DhoonHub.model.AuthResponse
import com.example.DhoonHub.model.LoginRequest
import com.example.DhoonHub.model.RegisterRequest
import com.example.DhoonHub.network.RetrofitProvider
import com.example.DhoonHub.storage.TokenStorage

class AuthRepository(context: Context) {
    private val authApi = RetrofitProvider.getAuthApi(context)
    private val tokenStorage = TokenStorage.getInstance(context)

    suspend fun login(userId: String, password: String): AuthResponse {
        val response = authApi.login(LoginRequest(user_id = userId, password = password))
        tokenStorage.setToken(response.token)
        return response
    }

    suspend fun register(username: String, userId: String, password: String): AuthResponse {
        val response = authApi.register(RegisterRequest(username = username, user_id = userId, password = password))
        tokenStorage.setToken(response.token)
        return response
    }

    fun logout() {
        tokenStorage.clear()
    }

    fun isLoggedIn(): Boolean = !tokenStorage.getToken().isNullOrBlank()
}