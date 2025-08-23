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

@Composable
fun RegisterScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { AuthRepository(context) }
    var username by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Register", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
            OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text("User ID") })
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
            if (error != null) Text(text = error!!, color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                isLoading = true
                error = null
                scope.launch {
                    try {
                        repo.register(username, userId, password)
                        nav.navigate("main") { popUpTo("register") { inclusive = true } }
                    } catch (e: Exception) {
                        error = e.message
                    } finally { isLoading = false }
                }
            }, enabled = !isLoading) { Text("Register") }
            TextButton(onClick = { nav.popBackStack() }) { Text("Already have an account? Login") }
        }
    }
}