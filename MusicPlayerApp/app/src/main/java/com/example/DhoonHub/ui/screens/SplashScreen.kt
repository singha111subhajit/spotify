package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.DhoonHub.utils.ConnectionStatus
import com.example.DhoonHub.utils.NetworkConnectivityObserver
import com.example.DhoonHub.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import android.util.Log

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
        Text("Checking network connection...")
    }

    LaunchedEffect(Unit) {
        val networkStatus = NetworkConnectivityObserver(context).observe().first()
        val token = com.example.DhoonHub.storage.TokenStorage.getInstance(context).getToken()
        Log.d("SplashScreen", "Network Status: $networkStatus, Token: $token")

        if (networkStatus == ConnectionStatus.Available) {
            if (token != null) {
                Log.d("SplashScreen", "Navigating to main (token exists)")
                navController.navigate("main") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                Log.d("SplashScreen", "Navigating to login (no token)")
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        } else {
            Log.d("SplashScreen", "Navigating to offline (no network)")
            navController.navigate("offline") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }
}
