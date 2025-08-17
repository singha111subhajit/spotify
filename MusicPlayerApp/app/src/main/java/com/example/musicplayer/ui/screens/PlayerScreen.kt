package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.musicplayer.player.MusicPlayerService
import coil.compose.AsyncImage

@Composable
fun PlayerScreen(nav: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(240_000L) } // placeholder

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        // Artwork placeholder
        AsyncImage(model = null, contentDescription = null, modifier = Modifier.fillMaxWidth().height(260.dp))
        Spacer(Modifier.height(16.dp))
        Text("Now Playing", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        // Seek bar
        Slider(
            value = (position / duration.toFloat()).coerceIn(0f, 1f),
            onValueChange = { fraction -> position = (fraction * duration).toLong() },
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position))
            Text(formatTime(duration))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { MusicPlayerService.sendControl(context, MusicPlayerService.ACTION_PREVIOUS) }) { Text("Prev") }
            Button(onClick = {
                MusicPlayerService.sendControl(context, MusicPlayerService.ACTION_TOGGLE_PLAY_PAUSE)
                isPlaying = !isPlaying
            }) { Text(if (isPlaying) "Pause" else "Play") }
            Button(onClick = { MusicPlayerService.sendControl(context, MusicPlayerService.ACTION_NEXT) }) { Text("Next") }
        }
        Spacer(Modifier.height(16.dp))
        // Equalizer placeholder
        Text("Equalizer", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AssistChip(onClick = { /* set preset */ }, label = { Text("Pop") })
            AssistChip(onClick = { /* set preset */ }, label = { Text("Rock") })
            AssistChip(onClick = { /* set preset */ }, label = { Text("Jazz") })
            AssistChip(onClick = { /* set preset */ }, label = { Text("Classical") })
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}