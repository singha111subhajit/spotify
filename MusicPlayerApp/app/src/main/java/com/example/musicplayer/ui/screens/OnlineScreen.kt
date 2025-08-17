package com.example.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.musicplayer.repository.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineScreen(rootNav: NavController) {
    val context = LocalContext.current
    val repo = remember { MusicRepository(context) }
    val authRepo = remember { AuthRepository(context) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var defaultPlaylistId by remember { mutableStateOf<Int?>(null) }
    var playlistSongIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    suspend fun load() {
        isLoading = true
        error = null
        runCatching { repo.getSongsOnline() }
            .onSuccess { songs = it }
            .onFailure { error = it.message }
        runCatching { repo.getPlaylistsOnline() }.onSuccess { pls ->
            val pid = pls.firstOrNull()?.id
            defaultPlaylistId = pid
            if (pid != null) {
                val psongs = runCatching { repo.getPlaylistSongs(pid) }.getOrDefault(emptyList())
                playlistSongIds = psongs.map { it.song_id }.toSet()
            }
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Songs") }, navigationIcon = {
        IconButton(onClick = { rootNav.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
    }, actions = {
        TextButton(onClick = {
            authRepo.logout()
            rootNav.navigate("login") { popUpTo("main") { inclusive = true } }
        }) { Text("Logout") }
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
                    val inPlaylist = remember(playlistSongIds, song.id, song.title) {
                        val sid = song.id ?: song.title
                        playlistSongIds.contains(sid)
                    }
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
                                val pid = defaultPlaylistId
                                if (pid != null) {
                                    if (inPlaylist) {
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                val psongs = repo.getPlaylistSongs(pid)
                                                val toRemove = psongs.find { it.song_id == (song.id ?: song.title) }
                                                if (toRemove != null) {
                                                    repo.removeSongFromPlaylist(pid, toRemove.id)
                                                    playlistSongIds = playlistSongIds - toRemove.song_id
                                                    snackbarHostState.showSnackbar("Removed from playlist")
                                                }
                                            }
                                        }) { Text("Remove") }
                                    } else {
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                val sid = song.id ?: song.title
                                                repo.addSongToPlaylist(pid, sid, song.title)
                                                playlistSongIds = playlistSongIds + sid
                                                snackbarHostState.showSnackbar("Added to playlist")
                                            }
                                        }) { Text("Add") }
                                    }
                                }
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