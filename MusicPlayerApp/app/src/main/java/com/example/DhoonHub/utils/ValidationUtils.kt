package com.example.DhoonHub.utils

import android.util.Patterns
import java.util.regex.Pattern

object ValidationUtils {
    
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun isValidPassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Error("Password cannot be empty")
            password.length < 6 -> ValidationResult.Error("Password must be at least 6 characters")
            password.length > 50 -> ValidationResult.Error("Password must be less than 50 characters")
            !password.any { it.isDigit() } -> ValidationResult.Error("Password must contain at least one digit")
            !password.any { it.isLetter() } -> ValidationResult.Error("Password must contain at least one letter")
            else -> ValidationResult.Success
        }
    }
    
    fun isValidUsername(username: String): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Error("Username cannot be empty")
            username.length < 3 -> ValidationResult.Error("Username must be at least 3 characters")
            username.length > 20 -> ValidationResult.Error("Username must be less than 20 characters")
            !Pattern.matches("^[a-zA-Z0-9_]+$", username) -> ValidationResult.Error("Username can only contain letters, numbers, and underscores")
            else -> ValidationResult.Success
        }
    }
    
    fun isValidName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Name cannot be empty")
            name.length < 2 -> ValidationResult.Error("Name must be at least 2 characters")
            name.length > 50 -> ValidationResult.Error("Name must be less than 50 characters")
            !Pattern.matches("^[a-zA-Z\\s]+$", name) -> ValidationResult.Error("Name can only contain letters and spaces")
            else -> ValidationResult.Success
        }
    }
    
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
