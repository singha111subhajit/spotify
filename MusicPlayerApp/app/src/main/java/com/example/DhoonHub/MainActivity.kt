
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
import com.example.DhoonHub.ui.MainScaffold
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
            val context = LocalContext.current

            // Create a single MusicViewModel instance to be shared across screens.
            // This ensures that the cache and state are preserved during navigation.
            val musicViewModel: com.example.DhoonHub.viewmodel.MusicViewModel = viewModel(
                factory = com.example.DhoonHub.viewmodel.MusicViewModel.Factory(context)
            )
            
            
            
            NavHost(navController = navController, startDestination = "login") {
                composable("login") { LoginScreen(navController) }
                composable("register") { RegisterScreen(navController) }
                composable("main") { MainScaffold(rootNavController = navController, musicViewModel = musicViewModel) }
                composable("player") { PlayerScreen(navController) }
                composable(
                    route = "album/{albumName}",
                    arguments = listOf(navArgument("albumName") { type = NavType.StringType })
                ) { backStackEntry ->
                    val albumName = backStackEntry.arguments?.getString("albumName")
                    if (albumName != null) {
                        // Use the shared ViewModel instance
                        AlbumScreen(navController = navController, rootNavController = navController, albumName = albumName, musicViewModel = musicViewModel)
                    } else {
                        // Handle the case where albumName is null, e.g., show an error or navigate back
                        Text("Error: Album not found") // Or some other error handling
                    }
                }
            }
        }
    }
}

data class BottomItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
