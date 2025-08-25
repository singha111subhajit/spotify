package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.DhoonHub.repository.AuthRepository
import com.example.DhoonHub.storage.SettingsStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(rootNav: NavController) {
    val context = LocalContext.current
    val authRepo = remember { AuthRepository(context) }
    val settings = remember { SettingsStorage.getInstance(context) }
    
    // Use a default username
    var username by remember { mutableStateOf("User") }
    
    // Try to get user information when the screen loads
    LaunchedEffect(Unit) {
        // Since we don't have a getUserId method, we'll just use a default value
        // In a real app, you might want to store the username during login/registration
        // and retrieve it here from SharedPreferences or another storage mechanism
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile avatar
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile Picture",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Username
        Text(
            text = username,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}