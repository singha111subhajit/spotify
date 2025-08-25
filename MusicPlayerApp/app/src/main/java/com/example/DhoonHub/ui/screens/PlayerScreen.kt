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

    val retrofit = remember { RetrofitProvider.getRetrofit(context) }
    val musicApi = remember { retrofit.create(MusicApi::class.java) }
    val repo = remember { MusicRepository(context) }

    var related by remember { mutableStateOf(listOf<Song>()) }
    val scope = rememberCoroutineScope()

    var isDownloaded by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }

    // ✅ NEW: collapsible related section
    var relatedExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.currentUrl, uiState.title, uiState.artist) {
        val url = uiState.currentUrl ?: return@LaunchedEffect
        isDownloaded = if (url.startsWith("http")) {
            val song = Song(id = url, title = uiState.title, artist = uiState.artist, url = url)
            repo.isSongDownloaded(song)
        } else true
    }

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
            AsyncImage(
                model = uiState.artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_music_note),
                error = painterResource(R.drawable.ic_music_note)
            )

            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Player controls
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { DhoonHubService.sendControl(context, DhoonHubService.ACTION_TOGGLE_SHUFFLE) }) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (uiState.isShuffle) MaterialTheme.colorScheme.primary else Color.White)
                        }

                        IconButton(onClick = {
                            DhoonHubService.sendControl(context, DhoonHubService.ACTION_PREVIOUS)
                        }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        ElevatedButton(onClick = {
                            DhoonHubService.sendControl(context, DhoonHubService.ACTION_TOGGLE_PLAY_PAUSE)
                        }) {
                            Icon(
                                if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        IconButton(onClick = {
                            DhoonHubService.sendControl(context, DhoonHubService.ACTION_NEXT)
                        }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        IconButton(onClick = { DhoonHubService.sendControl(context, DhoonHubService.ACTION_TOGGLE_REPEAT) }) {
                            Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = if (uiState.isRepeat) MaterialTheme.colorScheme.primary else Color.White)
                        }

                        val currentUrl = uiState.currentUrl
                        IconButton(onClick = {
                            val song = Song(
                                id = currentUrl,
                                title = uiState.title,
                                artist = uiState.artist,
                                url = currentUrl ?: "",
                                thumbnail = uiState.artworkUrl
                            )
                            if (!song.url.startsWith("http") || isDownloaded || downloading) return@IconButton
                            downloading = true
                            scope.launch {
                                try {
                                    if (repo.downloadSong(song)) {
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
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ✅ Collapsible related songs section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "More by ${uiState.artist}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        IconButton(onClick = { relatedExpanded = !relatedExpanded }) {
                            Icon(
                                if (relatedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (relatedExpanded) "Collapse" else "Expand",
                                tint = Color.White
                            )
                        }
                    }

                    if (relatedExpanded) {
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
                                        IconButton(onClick = {
                                            DhoonHubService.startPlayUrl(
                                                context,
                                                listOf(song),
                                                0
                                            )
                                        }) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
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
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}