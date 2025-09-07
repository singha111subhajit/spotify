package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.DhoonHub.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.DhoonHub.viewmodel.AuthViewModel
import com.example.DhoonHub.storage.TokenStorage
import com.example.DhoonHub.model.RegisterRequest
import com.example.DhoonHub.utils.NetworkResult
import com.example.DhoonHub.ui.components.LoadingButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(nav: NavController, authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(AuthRepository(LocalContext.current), TokenStorage.getInstance(LocalContext.current)))) {
    var username by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val registerResult by authViewModel.registerResult.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(registerResult) {
        when (registerResult) {
            is NetworkResult.Loading -> {
                isLoading = true
                error = null
            }
            is NetworkResult.Success -> {
                isLoading = false
                nav.navigate("main") {
                    popUpTo("register") { inclusive = true }
                }
            }
            is NetworkResult.Error -> {
                isLoading = false
                error = (registerResult as NetworkResult.Error).message
            }
            else -> {
                isLoading = false
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Register", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
            OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text("User ID") })
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
            if (error != null) Text(text = error!!, color = MaterialTheme.colorScheme.error)
            LoadingButton(onClick = {
                if (username.isBlank() || userId.isBlank() || password.isBlank()) {
                    error = "Please fill in all fields"
                    return@LoadingButton
                }
                authViewModel.register(RegisterRequest(username, userId.trim(), password))
            }, enabled = !isLoading, isLoading = isLoading) { Text("Register") }
            TextButton(onClick = { nav.popBackStack() }) { Text("Already have an account? Login") }
        }
    }
}