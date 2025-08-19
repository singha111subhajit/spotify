package com.example.DhoonHub.network.api

import com.example.DhoonHub.model.AuthResponse
import com.example.DhoonHub.model.LoginRequest
import com.example.DhoonHub.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse
}