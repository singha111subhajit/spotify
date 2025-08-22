
package com.example.DhoonHub

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.DhoonHub.player.PlaybackStateHolder
import com.example.DhoonHub.ui.components.MiniPlayer
import com.example.DhoonHub.ui.screens.*
import com.example.DhoonHub.ui.theme.MusicAppTheme

class MainActivity : ComponentActivity() {
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { AppNav() }
    }
}

@Composable
fun AppNav() {
    MusicAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val navController: NavHostController = rememberNavController()
            
            // Collect playback state to know if music is playing
            val playbackState by PlaybackStateHolder.uiState.collectAsState()
            
            NavHost(navController = navController, startDestination = "login") {
                composable("login") { LoginScreen(navController) }
                composable("register") { RegisterScreen(navController) }
                composable("main") { MainScaffold(rootNavController = navController) }
                composable("player") { PlayerScreen(navController) }
                composable(
                    route = "album/{albumName}",
                    arguments = listOf(navArgument("albumName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val albumName = backStackEntry.arguments?.getString("albumName")
                    if (albumName != null) {
                        val context = LocalContext.current
                        val musicViewModel = viewModel<com.example.DhoonHub.viewmodel.MusicViewModel>(
                            factory = com.example.DhoonHub.viewmodel.MusicViewModel.Factory(context)
                        )
                        AlbumScreen(rootNav = navController, albumName = albumName, musicViewModel = musicViewModel)
                    } else {
                        // Handle the case where albumName is null, e.g., show an error or navigate back
                        Text("Error: Album not found") // Or some other error handling
                    }
                }
            }
        }
    }
}

@Composable
fun MainScaffold(rootNavController: NavController) {
    val bottomNavController = rememberNavController()
    val items = listOf(
        BottomItem("home", "Home", Icons.Default.Home),
        BottomItem("search", "Search", Icons.Default.Search),
        BottomItem("library", "Library", Icons.Default.LibraryMusic),
    )
    
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isPlayerScreen = currentDestination?.route == "player"
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination?.route == item.route,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        // Main content
        NavHost(bottomNavController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(rootNavController) }
            composable("search") { SearchScreen(rootNavController) }
            composable("library") { LibraryScreen(rootNavController) }
        }
        
        // Add MiniPlayer if not on player screen
        if (!isPlayerScreen) {
            MiniPlayer(navController = rootNavController)
        }
    }
}

data class BottomItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
