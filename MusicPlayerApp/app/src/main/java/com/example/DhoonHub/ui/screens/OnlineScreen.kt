package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import com.example.DhoonHub.R
import com.example.DhoonHub.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineScreen(
    songs: List<Song>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    isSearching: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    isSongDownloaded: (Song) -> Boolean,
    downloadingSongs: Set<String>,
    canLoadMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit
) {
    val filteredSongs = songs
    
    if (isLoading) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (error != null) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else if (songs.isEmpty()) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Search for songs to get results from DhoonHub",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
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
                    onValueChange = { onSearchQueryChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true
                )
                
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
                
                if (!isSearching) {
                    Text(
                        text = "${filteredSongs.size} song${if (filteredSongs.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                
                if (filteredSongs.isNotEmpty()) {
                    val undownloadedSongs = filteredSongs.filter { !isSongDownloaded(it) }
                    if (undownloadedSongs.isNotEmpty()) {
                        Button(
                            onClick = {
                                undownloadedSongs.forEach { song ->
                                    onDownloadClick(song)
                                }
                            },
                            enabled = downloadingSongs.isEmpty()
                        ) {
                            Text("Download All (${undownloadedSongs.size})")
                        }
                    }
                }
                
                
            }
            
            if (searchQuery.isNotBlank() && filteredSongs.isNotEmpty()) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        items(filteredSongs.take(6)) { s ->
                            ListItem(
                                leadingContent = {
                                    AsyncImage(
                                        model = s.thumbnail,
                                        contentDescription = s.title,
                                        modifier = Modifier.size(40.dp),
                                        placeholder = painterResource(R.drawable.ic_music_note),
                                        error = painterResource(R.drawable.ic_music_note)
                                    )
                                },
                                headlineContent = { Text(s.title, maxLines = 1) },
                                supportingContent = { Text(s.artist, maxLines = 1) },
                                trailingContent = {
                                    TextButton(onClick = { onSongClick(s) }) { Text("Play") }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
            if (isSearching) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredSongs.isEmpty() && searchQuery.isNotEmpty()) {
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
                        OnlineSongItem(
                            song = song,
                            onPlayClick = { onSongClick(song) },
                            onDownloadClick = { onDownloadClick(song) },
                            isSongDownloaded = isSongDownloaded(song),
                            isDownloading = downloadingSongs.contains(song.id.orEmpty())
                        )
                    }
                    item(key = "load-more") {
                        if (filteredSongs.isNotEmpty() && canLoadMore && !isSearching) {
                            LaunchedEffect(filteredSongs.size, searchQuery) {
                                onLoadMore()
                            }
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineSongItem(
    song: Song,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    isSongDownloaded: Boolean,
    isDownloading: Boolean
) {
    ElevatedCard(
        Modifier.fillMaxWidth()
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
                    placeholder = painterResource(R.drawable.ic_music_note),
                    error = painterResource(R.drawable.ic_music_note)
                )
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    song.album?.let { album ->
                        Text(
                            text = album,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    
                    IconButton(
                        onClick = onDownloadClick,
                        enabled = !isSongDownloaded && !isDownloading
                    ) {
                        Icon(
                            if (isSongDownloaded) Icons.Default.Check else Icons.Default.Download,
                            contentDescription = if (isSongDownloaded) "Already Downloaded" else "Download",
                            tint = if (isSongDownloaded) MaterialTheme.colorScheme.primary 
                                  else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(onClick = onPlayClick) {
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
private fun CircularProgressAnimated(isVisible: Boolean) {
    if (isVisible) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.dp
        )
    }
}