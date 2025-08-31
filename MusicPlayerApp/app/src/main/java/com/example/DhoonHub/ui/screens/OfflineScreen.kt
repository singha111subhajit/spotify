package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import com.example.DhoonHub.R
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.DhoonHub.model.Song

@Composable
fun OfflineScreen(songs: List<Song>, onSongClick: (Song) -> Unit, onDeleteSong: (Song) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSongs = if (searchQuery.isEmpty()) {
        songs
    } else {
        songs.filter { song ->
            song.title.contains(searchQuery, ignoreCase = true) ||
            song.artist.contains(searchQuery, ignoreCase = true)
        }
    }
    
    if (songs.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "No offline songs",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "No offline songs",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Download songs from the Online tab to listen offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            Column(
                Modifier.fillMaxSize()
            ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search offline songs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true
                )
                
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
                
                Text(
                    text = "${filteredSongs.size} song${if (filteredSongs.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            
            if (filteredSongs.isEmpty() && searchQuery.isNotEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "No results",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No songs found matching '$searchQuery'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSongs) { song ->
                        OfflineSongItem(
                            song = song, 
                            onClick = { onSongClick(song) },
                            onDelete = { onDeleteSong(song) }
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
fun OfflineSongItem(song: Song, onClick: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = song.thumbnail,
                    contentDescription = song.title,
                    modifier = Modifier.size(56.dp),
                    placeholder = painterResource(R.drawable.ic_launcher_foreground),
                    error = painterResource(R.drawable.ic_launcher_foreground)
                )
                val meta by rememberOfflineMetadata(song.url)
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = meta.title?.takeIf { it.isNotBlank() } ?: song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = meta.artist?.takeIf { it.isNotBlank() } ?: song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmbeddedArtImage(filePath: String, size: Dp) {
    val bitmapState = produceState<android.graphics.Bitmap?>(initialValue = null, filePath) {
        value = withContext(Dispatchers.IO) {
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(filePath)
                val art = mmr.embeddedPicture
                mmr.release()
                if (art != null) BitmapFactory.decodeByteArray(art, 0, art.size) else null
            } catch (e: Exception) {
                null
            }
        }
    }
    if (bitmapState.value != null) {
        Image(
            bitmap = bitmapState.value!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(size)
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size)
        )
    }
}

data class OfflineMeta(val title: String?, val artist: String?, val album: String?)

@Composable
private fun rememberOfflineMetadata(filePath: String): State<OfflineMeta> {
    return produceState(initialValue = OfflineMeta(null, null, null), filePath) {
        val meta = withContext(Dispatchers.IO) {
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(filePath)
                val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                mmr.release()
                OfflineMeta(title, artist, album)
            } catch (e: Exception) {
                OfflineMeta(null, null, null)
            }
        }
        value = meta
    }
}
