package com.example.DhoonHub.model

data class LoginRequest(
    val user_id: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val user_id: String,
    val password: String
)

data class AuthResponse(
    val token: String?
)