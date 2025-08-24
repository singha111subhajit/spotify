package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.DhoonHub.model.Song
import com.example.DhoonHub.player.DhoonHubService
import com.example.DhoonHub.repository.MusicRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineScreen(rootNav: NavController) {
    val context = LocalContext.current
    val repo = remember { MusicRepository(context) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(Unit) { songs = repo.getOfflineSongsWithMetadata() }

    Scaffold(topBar = { TopAppBar(title = { Text("Offline") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(songs) { song ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                DhoonHubService.startPlayFile(context, song)
                                rootNav.navigate("player")
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = {
                            DhoonHubService.startPlayFile(context, song)
                            rootNav.navigate("player")
                        }) { Text("Play") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}