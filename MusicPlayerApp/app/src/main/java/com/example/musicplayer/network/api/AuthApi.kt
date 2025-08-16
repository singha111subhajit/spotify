package com.example.musicplayer.network.api

import com.example.musicplayer.model.AuthResponse
import com.example.musicplayer.model.LoginRequest
import com.example.musicplayer.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse
}