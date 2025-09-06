package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
    onlineSongs: List<Song>,
    searchResults: List<Song>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    isSearching: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    isSongDownloaded: (Song) -> Boolean,
    downloadingSongs: Set<String>,
    selectedSongIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    loadMoreOnlineSongs: () -> Unit,
    isPaginatingOnlineSongs: Boolean
) {
    val songsToShow = if (searchQuery.isBlank()) onlineSongs else searchResults
    val listState = rememberLazyListState()

    Column(Modifier.fillMaxSize()) {
        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search for songs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        }

        // Loading Indicator
        if (isLoading || isSearching) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Error Message
        error?.let {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Results / Empty State
        if (!isLoading && !isSearching && error == null) {
            if (songsToShow.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (searchQuery.isBlank()) "No online songs found."
                            else "No songs found matching '$searchQuery'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Results List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header with song count and download all button
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${songsToShow.size} song${if (songsToShow.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val selectedSongs = songsToShow.filter { selectedSongIds.contains(it.id) }
                            if (selectedSongs.isNotEmpty()) {
                                Button(
                                    onClick = { selectedSongs.forEach { onDownloadClick(it) } },
                                    enabled = downloadingSongs.isEmpty()
                                ) {
                                    Text("Download Selected (${selectedSongs.size})")
                                }
                            } else {
                                val undownloadedSongs = songsToShow.filter { !isSongDownloaded(it) }
                                if (undownloadedSongs.isNotEmpty()) {
                                    Button(
                                        onClick = { undownloadedSongs.forEach { onDownloadClick(it) } },
                                        enabled = downloadingSongs.isEmpty()
                                    ) {
                                        Text("Download All (${undownloadedSongs.size})")
                                    }
                                }
                            }
                        }
                    }

                    items(songsToShow) { song ->
                        OnlineSongItem(
                            song = song,
                            isSelected = selectedSongIds.contains(song.id),
                            onToggleSelection = { onToggleSelection(song.id.orEmpty()) },
                            onPlayClick = { onSongClick(song) },
                            onDownloadClick = { onDownloadClick(song) },
                            isSongDownloaded = isSongDownloaded(song),
                            isDownloading = downloadingSongs.contains(song.id.orEmpty())
                        )
                    }

                    if (isPaginatingOnlineSongs) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }

                LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
                    val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                    if (lastVisibleItem != null && lastVisibleItem.index >= songsToShow.size - 5) {
                        loadMoreOnlineSongs()
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineSongItem(
    song: Song,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
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
            Checkbox(checked = isSelected, onCheckedChange = if (isSongDownloaded) { _: Boolean -> } else { _ -> onToggleSelection() })
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
