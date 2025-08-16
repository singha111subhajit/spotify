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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { MusicRepository(context) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        runCatching { repo.getSongsOnline() }
            .onSuccess { songs = it }
            .onFailure { }
        isLoading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Online") }) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(songs) { song ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { /* open details or player */ }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.titleMedium)
                            Text(song.artist, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                MusicPlayerService.startPlayUrl(context, song.url)
                                nav.navigate("player")
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
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}