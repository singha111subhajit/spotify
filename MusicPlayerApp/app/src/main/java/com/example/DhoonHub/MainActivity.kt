
package com.example.DhoonHub

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.DhoonHub.ui.MainScaffold
import com.example.DhoonHub.ui.screens.AlbumScreen
import com.example.DhoonHub.ui.screens.LoginScreen
import com.example.DhoonHub.ui.screens.OfflineScreen
import com.example.DhoonHub.ui.screens.PlayerScreen
import com.example.DhoonHub.ui.screens.RegisterScreen
import com.example.DhoonHub.ui.screens.SplashScreen
import com.example.DhoonHub.ui.theme.MusicAppTheme
import com.example.DhoonHub.utils.ConnectionStatus
import com.example.DhoonHub.utils.NetworkConnectivityObserver
import kotlinx.coroutines.launch

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
        val navController: NavHostController = rememberNavController()
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val networkStatus by remember(context) {
            NetworkConnectivityObserver(context).observe()
        }.collectAsState(initial = ConnectionStatus.Unavailable)

        val ConnectionStatusSaver = Saver<ConnectionStatus, String>(
            save = { status -> status.javaClass.simpleName },
            restore = {
                when (it) {
                    "Available" -> ConnectionStatus.Available
                    "Unavailable" -> ConnectionStatus.Unavailable
                    else -> ConnectionStatus.Unavailable // Default or error case
                }
            }
        )

        var wasOffline by rememberSaveable { mutableStateOf(false) }
        val previousNetworkStatusString = rememberSaveable { mutableStateOf(networkStatus.javaClass.simpleName) }

        LaunchedEffect(networkStatus) {
            if (networkStatus == ConnectionStatus.Unavailable && previousNetworkStatusString.value == ConnectionStatus.Available.javaClass.simpleName) {
                wasOffline = true
                navController.navigate("offline") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                }
            } else if (networkStatus == ConnectionStatus.Available && wasOffline) {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Internet connection restored! Go online?",
                        actionLabel = "Go Online",
                        duration = SnackbarDuration.Long
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> {
                            navController.navigate("main") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                            }
                        }
                        SnackbarResult.Dismissed -> {
                            // User dismissed the snackbar
                        }
                    }
                }
                wasOffline = false
            }
            previousNetworkStatusString.value = networkStatus.javaClass.simpleName
        }

        val musicViewModel: com.example.DhoonHub.viewmodel.MusicViewModel = viewModel(
            factory = com.example.DhoonHub.viewmodel.MusicViewModel.Factory(context)
        )

        val authRepository = com.example.DhoonHub.repository.AuthRepository(context)
        val tokenStorage = com.example.DhoonHub.storage.TokenStorage.getInstance(context)
        val authViewModel: com.example.DhoonHub.viewmodel.AuthViewModel = viewModel(
            factory = com.example.DhoonHub.viewmodel.AuthViewModel.Factory(authRepository, tokenStorage)
        )

        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") { SplashScreen(navController) }
                        composable("offline") { OfflineScreen(musicViewModel = musicViewModel, rootNav = navController) }
                        composable("login") { LoginScreen(navController, authViewModel) }
                        composable("register") { RegisterScreen(navController, authViewModel) }
                        composable("main") { MainScaffold(rootNavController = navController, musicViewModel = musicViewModel) }
                        composable("player") { PlayerScreen(navController) }
                        composable(
                            route = "album/{albumName}",
                            arguments = listOf(navArgument("albumName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val albumName = backStackEntry.arguments?.getString("album")
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
    }
}

data class BottomItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
