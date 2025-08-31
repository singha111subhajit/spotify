package com.example.DhoonHub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.DhoonHub.R
import com.example.DhoonHub.model.Song

@Composable
fun SongListItem(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = song.title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium),
                placeholder = painterResource(R.drawable.ic_music_note),
                error = painterResource(R.drawable.ic_music_note),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
