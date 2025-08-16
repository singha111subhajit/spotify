package com.example.musicplayer.ui.screens

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
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.repository.MusicRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { MusicRepository(context) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        runCatching { repo.getPlaylistsOnline() }
            .onSuccess { playlists = it }
            .onFailure { }
        isLoading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Playlists") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.fillMaxSize()) {
                items(playlists) { pl ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate("online") }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(pl.name, style = MaterialTheme.typography.titleMedium)
                            Text("${'$'}{pl.songs.size} songs", style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = { nav.navigate("online") }) { Text("Open") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}