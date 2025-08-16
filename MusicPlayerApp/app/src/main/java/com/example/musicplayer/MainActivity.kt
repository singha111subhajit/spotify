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
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.screens.LoginScreen
import com.example.musicplayer.ui.screens.RegisterScreen
import com.example.musicplayer.ui.screens.OnlineScreen
import com.example.musicplayer.ui.screens.OfflineScreen
import com.example.musicplayer.ui.screens.PlayerScreen
import com.example.musicplayer.ui.screens.PlaylistsScreen
import com.example.musicplayer.ui.theme.MusicAppTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue

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
                composable("main") { MainScaffold() }
                composable("player") { PlayerScreen(navController) }
            }
        }
    }
}

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val items = listOf(
        BottomItem("home", "Home"),
        BottomItem("search", "Search"),
        BottomItem("library", "Library"),
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination?.route == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(painterResource(id = android.R.drawable.ic_media_play), contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home") {
            composable("home") { OnlineScreen(navController) }
            composable("search") { com.example.musicplayer.ui.screens.SearchScreen(navController) }
            composable("library") { OfflineScreen(navController) }
        }
    }
}

data class BottomItem(val route: String, val label: String)