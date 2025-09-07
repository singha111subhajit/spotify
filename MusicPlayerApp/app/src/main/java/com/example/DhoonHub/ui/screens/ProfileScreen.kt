package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
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
import android.widget.Toast
import com.example.DhoonHub.viewmodel.AuthViewModel // Added import
import com.example.DhoonHub.ui.Screen // Added import for navigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    rootNavController: NavController,
    authViewModel: AuthViewModel
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState() // Observe isAuthenticated
    val context = LocalContext.current
    val authRepo = remember { AuthRepository(context) } // Keep authRepo for username update
    val settings = remember { SettingsStorage.getInstance(context) }

    var currentUsername by remember { mutableStateOf(authRepo.getUsername() ?: "DhoonHub User") }
    var editableUsername by remember { mutableStateOf(currentUsername) }
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    if (isAuthenticated) { // Only show logout if authenticated
                        TextButton(
                            onClick = {
                                authViewModel.logout() // Call logout from authViewModel
                                rootNavController.navigate("login") { // Navigate to login
                                    popUpTo(rootNavController.graph.startDestinationId) { // Correct popUpTo
                                        inclusive = true
                                    }
                                }
                            }
                        ) {
                            Text("Logout")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Center content
        ) {
            if (isAuthenticated) {
                // Display user profile information
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editableUsername,
                                    onValueChange = { editableUsername = it },
                                    label = { Text("Username") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    if (editableUsername.isNotBlank()) {
                                        authRepo.updateUsername(editableUsername)
                                        currentUsername = editableUsername
                                        isEditing = false
                                        Toast.makeText(context, "Username updated!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Text("Save")
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentUsername,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { isEditing = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Username")
                                    }
                                }
                            }
                            Text(
                                text = "Music Lover",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Settings card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current Language: ${settings.getLanguage()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Account card (Logout button)
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Account",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                authViewModel.logout() // Call logout from authViewModel
                                rootNavController.navigate("login") { // Navigate to login
                                    popUpTo(rootNavController.graph.startDestinationId) { // Correct popUpTo
                                        inclusive = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Logout")
                        }
                    }
                }
            } else {
                // Display login/register options if not authenticated
                Text("Please Login or Register")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    rootNavController.navigate("login")
                }) {
                    Text("Login")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    rootNavController.navigate("register")
                }) {
                    Text("Register")
                }
            }
        }
    }
}