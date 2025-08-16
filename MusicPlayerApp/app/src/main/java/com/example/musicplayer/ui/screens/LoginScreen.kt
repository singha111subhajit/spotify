package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.musicplayer.repository.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoginScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { AuthRepository(context) }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (repo.isLoggedIn()) nav.navigate("main") { popUpTo("login") { inclusive = true } }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Login", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text("User ID") })
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
            if (error != null) Text(text = error!!, color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                isLoading = true
                error = null
                scope.launch {
                    try {
                        repo.login(userId, password)
                        nav.navigate("main") { popUpTo("login") { inclusive = true } }
                    } catch (e: Exception) {
                        error = e.message
                    } finally { isLoading = false }
                }
            }, enabled = !isLoading) { Text("Login") }
            TextButton(onClick = { nav.navigate("register") }) { Text("No account? Register") }
        }
    }
}