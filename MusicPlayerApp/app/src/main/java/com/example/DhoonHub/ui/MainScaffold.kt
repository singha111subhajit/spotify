
package com.example.DhoonHub.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.DhoonHub.ui.components.MiniPlayer
import com.example.DhoonHub.ui.screens.HomeScreen
import com.example.DhoonHub.ui.screens.LibraryScreen
import com.example.DhoonHub.ui.screens.ProfileScreen
import com.example.DhoonHub.ui.screens.SearchScreen
import com.example.DhoonHub.ui.screens.AlbumScreen
import com.example.DhoonHub.ui.screens.ArtistScreen
import com.example.DhoonHub.player.PlaybackStateHolder
import com.example.DhoonHub.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.DhoonHub.repository.AuthRepository
import com.example.DhoonHub.storage.SettingsStorage

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", Icons.Default.Home, "Home")
    object Library : Screen("library", Icons.Default.LibraryMusic, "Library")
    object Search : Screen("search", Icons.Default.Search, "Search")
    object Profile : Screen("profile", Icons.Default.Person, "Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val settings = remember { SettingsStorage.getInstance(context) }

    val languages = listOf("English", "Hindi", "Bengali", "Punjabi", "Tamil", "Telugu")
    var selectedLanguage by remember { mutableStateOf(settings.getLanguage()) }
    var expandedLanguageMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("DhoonHub Menu", modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                // Removed Home NavigationDrawerItem
                // Removed Logout NavigationDrawerItem
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Language selection dropdown
                        IconButton(onClick = { expandedLanguageMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Select Language")
                        }
                        DropdownMenu(
                            expanded = expandedLanguageMenu,
                            onDismissRequest = { expandedLanguageMenu = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        selectedLanguage = lang
                                        settings.setLanguage(lang)
                                        expandedLanguageMenu = false
                                        Toast.makeText(context, "Language set to $lang", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        // Clear Cache dropdown
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Server Cache") },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        try {
                                            val response = musicViewModel.clearAlbumsCache()
                                            Toast.makeText(context, response, Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error clearing cache: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Artist Cache") },
                                onClick = {
                                    showMenu = false
                                    musicViewModel.clearArtistCache()
                                    Toast.makeText(context, "Artist cache cleared", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                )
            },
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
                    composable(Screen.Home.route) { HomeScreen(navController, rootNavController, musicViewModel) }
                    composable(Screen.Search.route) { SearchScreen(rootNavController, musicViewModel) }
                    composable(Screen.Library.route) { LibraryScreen(rootNavController, musicViewModel.musicRepository) }
                    composable(Screen.Profile.route) { ProfileScreen(rootNavController) }
                    composable("album/{albumName}") { backStackEntry ->
                        val albumName = backStackEntry.arguments?.getString("albumName") ?: ""
                        AlbumScreen(navController = navController, rootNavController = rootNavController, albumName = albumName, musicViewModel = musicViewModel)
                    }
                    composable("artist/{artistName}") { backStackEntry ->
                        val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
                        ArtistScreen(artistName = artistName, rootNavController = rootNavController, musicViewModel = musicViewModel, playbackState = PlaybackStateHolder.uiState.collectAsState().value)
                    }
                }

                // Add MiniPlayer at the bottom
                MiniPlayer(
                    navController = rootNavController,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                )
            }
        }
    }
}
