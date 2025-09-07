
package com.example.DhoonHub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.DhoonHub.model.LoginRequest
import com.example.DhoonHub.model.RegisterRequest
import com.example.DhoonHub.repository.AuthRepository
import com.example.DhoonHub.storage.TokenStorage
import com.example.DhoonHub.utils.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository, private val tokenStorage: TokenStorage) : ViewModel() {

    private val _loginResult = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.Idle())
    val loginResult: StateFlow<NetworkResult<Unit>> = _loginResult

    private val _registerResult = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.Idle())
    val registerResult: StateFlow<NetworkResult<Unit>> = _registerResult

    fun login(loginRequest: LoginRequest) {
        viewModelScope.launch {
            _loginResult.value = NetworkResult.Loading()
            when (val response = authRepository.login(loginRequest)) {
                is NetworkResult.Success -> {
                    tokenStorage.setToken(response.data.token)
                    _loginResult.value = NetworkResult.Success(Unit)
                }
                is NetworkResult.Error -> {
                    _loginResult.value = NetworkResult.Error(response.message)
                }
                else -> {}
            }
        }
    }

    fun register(registerRequest: RegisterRequest) {
        viewModelScope.launch {
            _registerResult.value = NetworkResult.Loading()
            when (val response = authRepository.register(registerRequest)) {
                is NetworkResult.Success -> {
                    _registerResult.value = NetworkResult.Success(Unit)
                }
                is NetworkResult.Error -> {
                    _registerResult.value = NetworkResult.Error(response.message)
                }
                else -> {}
            }
        }
    }

    class Factory(private val authRepository: AuthRepository, private val tokenStorage: TokenStorage) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(authRepository, tokenStorage) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
