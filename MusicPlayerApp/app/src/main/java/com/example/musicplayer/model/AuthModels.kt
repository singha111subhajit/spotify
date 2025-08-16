package com.example.musicplayer.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: String? = null
)