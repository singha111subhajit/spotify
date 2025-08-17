package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.musicplayer.R
import com.example.musicplayer.player.MusicPlayerService
import com.example.musicplayer.player.PlaybackStateHolder
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(nav: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by PlaybackStateHolder.uiState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(uiState.title.ifBlank { "Now Playing" }) },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model = uiState.artworkUrl,
                contentDescription = uiState.title,
                modifier = Modifier.fillMaxWidth().height(280.dp),
                placeholder = painterResource(R.drawable.ic_music_note),
                error = painterResource(R.drawable.ic_music_note)
            )
            Spacer(Modifier.height(16.dp))
            Text(uiState.title.ifBlank { "Unknown Title" }, style = MaterialTheme.typography.titleLarge)
            Text(uiState.artist.ifBlank { "Unknown Artist" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(uiState.positionMs))
                Text(formatTime(uiState.durationMs))
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { MusicPlayerService.sendControl(context, MusicPlayerService.ACTION_PREVIOUS) }) { Text("Prev") }
                Button(onClick = {
                    MusicPlayerService.sendControl(context, MusicPlayerService.ACTION_TOGGLE_PLAY_PAUSE)
                }) { Text(if (uiState.isPlaying) "Pause" else "Play") }
                Button(onClick = { MusicPlayerService.sendControl(context, MusicPlayerService.ACTION_NEXT) }) { Text("Next") }
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