package com.example.musicplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.screens.LoginScreen
import com.example.musicplayer.ui.screens.RegisterScreen
import com.example.musicplayer.ui.screens.OnlineScreen
import com.example.musicplayer.ui.screens.OfflineScreen
import com.example.musicplayer.ui.screens.PlayerScreen
import com.example.musicplayer.ui.screens.PlaylistsScreen
import com.example.musicplayer.ui.theme.MusicAppTheme

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
            NavHost(navController = navController, startDestination = "login") {
                composable("login") { LoginScreen(navController) }
                composable("register") { RegisterScreen(navController) }
                composable("playlists") { PlaylistsScreen(navController) }
                composable("online") { OnlineScreen(navController) }
                composable("offline") { OfflineScreen(navController) }
                composable("player") { PlayerScreen(navController) }
            }
        }
    }
}