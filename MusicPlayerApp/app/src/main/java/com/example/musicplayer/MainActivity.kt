package com.example.DhoonHub

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
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
import com.example.DhoonHub.ui.screens.LoginScreen
import com.example.DhoonHub.ui.screens.RegisterScreen
import com.example.DhoonHub.ui.screens.LibraryScreen
import com.example.DhoonHub.ui.screens.PlayerScreen
import com.example.DhoonHub.ui.screens.SearchScreen
import com.example.DhoonHub.ui.screens.HomeScreen
import com.example.DhoonHub.ui.screens.AlbumScreen
import com.example.DhoonHub.ui.theme.MusicAppTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding

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
                composable("main") { MainScaffold(rootNavController = navController) }
                composable("player") { PlayerScreen(navController) }
                composable("album/{name}") { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    AlbumScreen(navController, name)
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
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
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
        NavHost(bottomNavController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(rootNavController) }
            composable("search") { SearchScreen(rootNavController) }
            composable("library") { LibraryScreen(rootNavController) }
        }
    }
}

data class BottomItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)