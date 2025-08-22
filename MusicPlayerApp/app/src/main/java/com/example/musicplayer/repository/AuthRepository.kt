package com.example.DhoonHub.repository

import android.content.Context
import android.util.Log
import com.example.DhoonHub.model.AuthResponse
import com.example.DhoonHub.model.LoginRequest
import com.example.DhoonHub.model.RegisterRequest
import com.example.DhoonHub.network.RetrofitProvider
import com.example.DhoonHub.storage.TokenStorage
import com.example.DhoonHub.utils.NetworkResult
import com.example.DhoonHub.utils.ValidationUtils
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AuthRepository(context: Context) {
    private val authApi = RetrofitProvider.getAuthApi(context)
    private val tokenStorage = TokenStorage.getInstance(context)
    
    companion object {
        private const val TAG = "AuthRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    suspend fun login(userId: String, password: String): NetworkResult<AuthResponse> {
        // Validate input
        if (!ValidationUtils.isValidEmail(userId)) {
            return NetworkResult.Error("Please enter a valid email address")
        }
        
        val passwordValidation = ValidationUtils.isValidPassword(password)
        if (passwordValidation is ValidationUtils.ValidationResult.Error) {
            return NetworkResult.Error(passwordValidation.message)
        }

        return executeWithRetry {
            val response = authApi.login(LoginRequest(user_id = userId, password = password))
            tokenStorage.setToken(response.token)
            NetworkResult.Success(response)
        }
    }

    suspend fun register(username: String, userId: String, password: String): NetworkResult<AuthResponse> {
        // Validate input
        val usernameValidation = ValidationUtils.isValidUsername(username)
        if (usernameValidation is ValidationUtils.ValidationResult.Error) {
            return NetworkResult.Error(usernameValidation.message)
        }
        
        if (!ValidationUtils.isValidEmail(userId)) {
            return NetworkResult.Error("Please enter a valid email address")
        }
        
        val passwordValidation = ValidationUtils.isValidPassword(password)
        if (passwordValidation is ValidationUtils.ValidationResult.Error) {
            return NetworkResult.Error(passwordValidation.message)
        }

        return executeWithRetry {
            val response = authApi.register(RegisterRequest(username = username, user_id = userId, password = password))
            tokenStorage.setToken(response.token)
            NetworkResult.Success(response)
        }
    }

    fun logout() {
        try {
            tokenStorage.clear()
            Log.d(TAG, "User logged out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
        }
    }

    fun isLoggedIn(): Boolean = !tokenStorage.getToken().isNullOrBlank()
    
    private suspend fun <T> executeWithRetry(
        maxAttempts: Int = MAX_RETRY_ATTEMPTS,
        operation: suspend () -> NetworkResult<T>
    ): NetworkResult<T> {
        repeat(maxAttempts) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                Log.e(TAG, "Attempt ${attempt + 1} failed", e)
                
                if (attempt == maxAttempts - 1) {
                    return handleException(e)
                }
                
                // Only retry on network errors, not on client errors
                if (e is HttpException && e.code() in 400..499) {
                    return handleException(e)
                }
                
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        return NetworkResult.Error("Maximum retry attempts exceeded")
    }
    
    private fun <T> handleException(exception: Exception): NetworkResult<T> {
        return when (exception) {
            is HttpException -> {
                val errorMessage = when (exception.code()) {
                    400 -> "Invalid request. Please check your input."
                    401 -> "Invalid credentials. Please try again."
                    403 -> "Access forbidden. Please contact support."
                    404 -> "Service not found. Please try again later."
                    409 -> "User already exists. Please try logging in."
                    422 -> "Invalid data provided. Please check your input."
                    429 -> "Too many requests. Please try again later."
                    in 500..599 -> "Server error. Please try again later."
                    else -> "Authentication failed. Please try again."
                }
                Log.e(TAG, "HTTP error ${exception.code()}: $errorMessage")
                NetworkResult.Error(errorMessage, exception.code())
            }
            is UnknownHostException -> {
                Log.e(TAG, "Network error: No internet connection", exception)
                NetworkResult.Error("No internet connection. Please check your network.")
            }
            is SocketTimeoutException -> {
                Log.e(TAG, "Network error: Request timeout", exception)
                NetworkResult.Error("Request timeout. Please try again.")
            }
            is IOException -> {
                Log.e(TAG, "Network error: IO exception", exception)
                NetworkResult.Error("Network error. Please check your connection.")
            }
            else -> {
                Log.e(TAG, "Unexpected error", exception)
                NetworkResult.Error("An unexpected error occurred. Please try again.")
            }
        }
    }
}
