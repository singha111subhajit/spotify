
package com.example.DhoonHub.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.DhoonHub.ui.components.MiniPlayer
import com.example.musicplayer.ui.screens.HomeScreen
import com.example.DhoonHub.ui.screens.LibraryScreen
import com.example.DhoonHub.ui.screens.ProfileScreen
import com.example.DhoonHub.ui.screens.SearchScreen
import com.example.DhoonHub.viewmodel.MusicViewModel

@Composable
fun MainScaffold(
    rootNavController: NavController,
    musicViewModel: MusicViewModel
) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Library,
        Screen.Profile
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = screen.icon, 
                                contentDescription = screen.label
                            ) 
                        },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) { HomeScreen(rootNavController, musicViewModel) }
                composable(Screen.Search.route) { SearchScreen(rootNavController) }
                composable(Screen.Library.route) { LibraryScreen(rootNavController) }
                composable(Screen.Profile.route) { ProfileScreen(rootNavController) }
            }
            
            // Add MiniPlayer at the bottom
            MiniPlayer(
                navController = rootNavController,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
            )
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", Icons.Default.Home, "Home")
    object Library : Screen("library", Icons.Default.LibraryMusic, "Library")
    object Search : Screen("search", Icons.Default.Search, "Search")
    object Profile : Screen("profile", Icons.Default.Person, "Profile")
}
