
package com.example.DhoonHub.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.DhoonHub.R
import com.example.DhoonHub.player.DhoonHubService
import com.example.DhoonHub.player.PlaybackStateHolder

@Composable
fun MiniPlayer(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackState by PlaybackStateHolder.uiState.collectAsState()
    
    // Don't show mini player if nothing is playing
    if (playbackState.currentUrl == null) {
        return
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { navController.navigate("player") },
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork
            AsyncImage(
                model = playbackState.artworkUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                placeholder = painterResource(R.drawable.ic_music_note),
                error = painterResource(R.drawable.ic_music_note)
            )
            
            // Title and artist
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = playbackState.title.ifBlank { "Unknown Title" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = playbackState.artist.ifBlank { "Unknown Artist" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Play/Pause button
            IconButton(
                onClick = {
                    DhoonHubService.sendControl(
                        context,
                        DhoonHubService.ACTION_TOGGLE_PLAY_PAUSE
                    )
                }
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}
