
package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.DhoonHub.R
import com.example.DhoonHub.model.Song
import com.example.DhoonHub.network.RetrofitProvider
import com.example.DhoonHub.network.api.MusicApi
import com.example.DhoonHub.player.DhoonHubService
import com.example.DhoonHub.player.PlaybackStateHolder
import com.example.DhoonHub.repository.MusicRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(nav: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by PlaybackStateHolder.uiState.collectAsState()

    // Networking & API
    val retrofit = remember { RetrofitProvider.getRetrofit(context) }
    val musicApi = remember { retrofit.create(MusicApi::class.java) }
    // Create the repository instance at the top level
    val repo = remember { MusicRepository(context) }

    var related by remember { mutableStateOf(listOf<Song>()) }
    val scope = rememberCoroutineScope()

    // Track download state
    var isDownloaded by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    
    // Check download state whenever the current URL changes
    LaunchedEffect(uiState.currentUrl, uiState.title, uiState.artist) {
        val url = uiState.currentUrl ?: return@LaunchedEffect
        if (url.startsWith("http")) {
            val song = Song(
                id = url,
                title = uiState.title,
                artist = uiState.artist,
                url = url
            )
            isDownloaded = repo.isSongDownloaded(song)
        } else {
            isDownloaded = true // Local file is already "downloaded"
        }
    }

    // Fetch related songs when artist changes
    LaunchedEffect(uiState.artist) {
        val q = uiState.artist.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        related = runCatching { musicApi.search(q).songs }.getOrDefault(emptyList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title.ifBlank { "Now Playing" }) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Background artwork
            AsyncImage(
                model = uiState.artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_music_note),
                error = painterResource(R.drawable.ic_music_note)
            )

            // Overlay
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top 70%: player controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.7f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        uiState.title.ifBlank { "Unknown Title" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        uiState.artist.ifBlank { "Unknown Artist" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Seek bar
                    val duration = uiState.durationMs.takeIf { it > 0 } ?: 1L
                    Slider(
                        value = (uiState.positionMs / duration.toFloat()).coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            val newPos = (fraction * duration).toLong()
                            PlaybackStateHolder.update(positionMs = newPos)
                        },
                        onValueChangeFinished = {
                            PlaybackStateHolder.seekTo(context, uiState.positionMs)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(uiState.positionMs), color = Color.White)
                        Text(formatTime(uiState.durationMs), color = Color.White)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            DhoonHubService.sendControl(context, DhoonHubService.ACTION_PREVIOUS)
                        }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                        }

                        ElevatedButton(onClick = {
                            DhoonHubService.sendControl(context, DhoonHubService.ACTION_TOGGLE_PLAY_PAUSE)
                        }) {
                            Icon(
                                if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Download button
                        // Remove this line as we already defined repo at the top
                        // val repo = remember { MusicRepository(context) }
                        val currentUrl = uiState.currentUrl
                        
                        IconButton(onClick = {
                            val url = currentUrl ?: return@IconButton
                            if (!url.startsWith("http") || isDownloaded || downloading) return@IconButton
                            
                            downloading = true
                            val song = Song(
                                id = url,
                                title = uiState.title,
                                artist = uiState.artist,
                                url = url,
                                thumbnail = uiState.artworkUrl
                            )
                            
                            scope.launch {
                                try {
                                    val success = repo.downloadSong(song)
                                    if (success) {
                                        isDownloaded = true
                                    }
                                } finally {
                                    downloading = false
                                }
                            }
                        }) {
                            when {
                                downloading -> CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                                isDownloaded -> Icon(Icons.Default.Check, contentDescription = "Downloaded", tint = Color.White)
                                else -> Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                            }
                        }

                        IconButton(onClick = {
                            DhoonHubService.sendControl(context, DhoonHubService.ACTION_NEXT)
                        }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Bottom 30%: related songs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                ) {
                    Text(
                        "More by ${uiState.artist}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(related) { song ->
                            ListItem(
                                leadingContent = {
                                    AsyncImage(
                                        model = song.thumbnail,
                                        contentDescription = song.title,
                                        modifier = Modifier.size(48.dp),
                                        placeholder = painterResource(R.drawable.ic_music_note),
                                        error = painterResource(R.drawable.ic_music_note)
                                    )
                                },
                                headlineContent = { Text(song.title) },
                                supportingContent = { Text(song.artist) },
                                trailingContent = {
                                    TextButton(onClick = {
                                        DhoonHubService.startPlayUrl(
                                            context,
                                            song.url,
                                            title = song.title,
                                            artist = song.artist,
                                            artworkUrl = song.thumbnail
                                        )
                                    }) { Text("Play") }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
