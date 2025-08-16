package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.musicplayer.player.MusicPlayerService

@Composable
fun PlayerScreen(nav: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Now Playing", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { MusicPlayerService.sendControl(nav.context, MusicPlayerService.ACTION_PREVIOUS) }) { Text("Prev") }
            Button(onClick = { MusicPlayerService.sendControl(nav.context, MusicPlayerService.ACTION_TOGGLE_PLAY_PAUSE) }) { Text("Play/Pause") }
            Button(onClick = { MusicPlayerService.sendControl(nav.context, MusicPlayerService.ACTION_NEXT) }) { Text("Next") }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { MusicPlayerService.sendControl(nav.context, MusicPlayerService.ACTION_TOGGLE_SHUFFLE) }) { Text("Shuffle") }
            OutlinedButton(onClick = { MusicPlayerService.sendControl(nav.context, MusicPlayerService.ACTION_TOGGLE_REPEAT) }) { Text("Repeat") }
        }
    }
}