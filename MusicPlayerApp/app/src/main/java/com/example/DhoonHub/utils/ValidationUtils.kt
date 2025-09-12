package com.example.DhoonHub.utils

object ValidationUtils {
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    fun isValidUsername(username: String): ValidationResult {
        return if (username.isNotBlank()) ValidationResult.Success
        else ValidationResult.Error("Username cannot be empty")
    }

    fun isValidPassword(password: String): ValidationResult {
        return if (password.length >= 6) ValidationResult.Success
        else ValidationResult.Error("Password must be at least 6 characters long")
    }
