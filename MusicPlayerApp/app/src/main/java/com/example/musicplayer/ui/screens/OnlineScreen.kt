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
import com.example.musicplayer.model.Song
import com.example.musicplayer.player.MusicPlayerService
import com.example.musicplayer.repository.MusicRepository
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineScreen(rootNav: NavController) {
    val context = LocalContext.current
    val repo = remember { MusicRepository(context) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    suspend fun load() {
        isLoading = true
        error = null
        runCatching { repo.getSongsOnline() }
            .onSuccess { songs = it }
            .onFailure { error = it.message }
        isLoading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Home") }, actions = {
        TextButton(onClick = { scope.launch { load() } }) { Text("Refresh") }
    }) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (error != null) {
                Text("Error: ${'$'}error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(songs) { song ->
                    ListItem(
                        leadingContent = {
                            AsyncImage(model = song.thumbnail, contentDescription = song.title, modifier = Modifier.size(56.dp))
                        },
                        headlineContent = { Text(song.title) },
                        supportingContent = { Text(song.artist) },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    MusicPlayerService.startPlayUrl(context, song.url,
                                        title = song.title, artist = song.artist, artworkUrl = song.thumbnail)
                                    rootNav.navigate("player")
                                }) { Text("Play") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        val file = repo.downloadSong(song)
                                        snackbarHostState.showSnackbar(
                                            if (file != null) "Downloaded: ${'$'}{file.name}" else "Download failed"
                                        )
                                    }
                                }) { Text("Download") }
                            }
                        },
                        modifier = Modifier.clickable {
                            MusicPlayerService.startPlayUrl(context, song.url,
                                title = song.title, artist = song.artist, artworkUrl = song.thumbnail)
                            rootNav.navigate("player")
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}