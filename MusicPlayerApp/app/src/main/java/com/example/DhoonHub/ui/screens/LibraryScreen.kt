package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// Pull-to-refresh removed to avoid extra dependency
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
import com.example.DhoonHub.player.DhoonHubService
import com.example.DhoonHub.repository.MusicRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    rootNav: NavController,
    repo: MusicRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentTab by remember { mutableStateOf(0) }
    var offlineSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var onlineSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadingSongs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadedSongIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var onlineSearchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var canLoadMore by remember { mutableStateOf(true) }

    // Function to refresh offline songs and downloaded state
    fun refreshOfflineSongs() {
        coroutineScope.launch {
            offlineSongs = repo.getOfflineSongsWithMetadata()
            downloadedSongIds = offlineSongs.mapNotNull { it.id }.toSet()
        }
    }
    
    // Function to handle song download with proper state updates
    fun downloadSong(song: Song) {
        if (downloadingSongs.contains(song.id.orEmpty())) return
        
        val songId = song.id.orEmpty()
        downloadingSongs = downloadingSongs + songId
        
        coroutineScope.launch {
            try {
                val success = repo.downloadSong(song)
                if (success) {
                    // Update downloaded songs list
                    refreshOfflineSongs()
                    // Add to downloaded IDs set
                    downloadedSongIds = downloadedSongIds + songId
                }
            } finally {
                // Remove from downloading set regardless of outcome
                downloadingSongs = downloadingSongs - songId
            }
        }
    }
    
    // Function to check if a song is downloaded
    fun isSongDownloaded(song: Song): Boolean {
        val songId = song.id ?: song.title
        return downloadedSongIds.contains(songId) || 
               offlineSongs.any { it.url == song.url || it.title == song.title }
    }
    
    // Initial load of offline songs and download states
    LaunchedEffect(Unit) {
        refreshOfflineSongs()
    }

    // Load offline songs
    LaunchedEffect(Unit) {
        refreshOfflineSongs()
    }

    // Load online content when Online tab is selected and when query changes
    LaunchedEffect(currentTab, onlineSearchQuery) {
        if (currentTab == 1) {
            error = null
            // If no query, show a default JioSaavn-driven list using a default query
            if (onlineSearchQuery.isBlank()) {
                // Kickstart with a default query to fetch JioSaavn songs
                isLoading = onlineSongs.isEmpty()
                isSearching = true
                try {
                    currentPage = 1
                    val first = repo.searchSongsOnline("Hindi", page = currentPage, perPage = 20)
                    onlineSongs = first
                    canLoadMore = first.isNotEmpty()
                } catch (e: Exception) {
                    error = "Failed to load online songs: ${e.message}"
                } finally {
                    isLoading = false
                    isSearching = false
                }
            } else {
                isSearching = true
                try {
                    currentPage = 1
                    val first = repo.searchSongsOnline(onlineSearchQuery, page = currentPage, perPage = 20)
                    onlineSongs = first
                    canLoadMore = first.isNotEmpty()
                } catch (e: Exception) {
                    error = "Search failed: ${e.message}"
                } finally {
                    isSearching = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Tab Row with song counts
            TabRow(selectedTabIndex = currentTab) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text("Offline (${offlineSongs.size})") },
                    icon = { Icon(Icons.Default.Download, contentDescription = "Offline") }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text("Online (${onlineSongs.size})") },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "Online") }
                )
            }

            // Content based on selected tab
            when (currentTab) {
                0 -> OfflineTab(
                    songs = offlineSongs,
                    onSongClick = { song ->
                        DhoonHubService.startPlayFile(context, song)
                        rootNav.navigate("player")
                    },
                    onDeleteSong = { song ->
                        // Find the corresponding file and delete it
                        val offlineFiles = repo.getOfflineSongs()
                        val fileToDelete = offlineFiles.find { file ->
                            val filename = file.nameWithoutExtension
                            val parts = filename.split("-")
                            val artist = if (parts.size > 1) parts[0] else "Unknown Artist"
                            val title = if (parts.size > 2) parts[1] else filename
                            artist == song.artist && title == song.title
                        }
                        fileToDelete?.delete()
                        refreshOfflineSongs()
                    }
                )
                1 -> OnlineTab(
                    songs = onlineSongs,
                    isLoading = isLoading,
                    error = error,
                    searchQuery = onlineSearchQuery,
                    isSearching = isSearching,
                    onSearchQueryChange = { q -> onlineSearchQuery = q },
                    onSongClick = { song ->
                        // Stream directly without auto-download
                        val startIndex = onlineSongs.indexOf(song)
                        DhoonHubService.startPlayUrl(
                            context,
                            songs = onlineSongs,
                            startIndex = startIndex
                        )
                        rootNav.navigate("player")
                    },
                    onDownloadClick = { song ->
                        downloadSong(song)
                    },
                    isSongDownloaded = { song -> isSongDownloaded(song) },
                    downloadingSongs = downloadingSongs,
                    canLoadMore = canLoadMore,
                    isLoadingMore = isLoadingMore,
                    onLoadMore = {
                        if (canLoadMore && !isSearching) {
                            coroutineScope.launch {
                                isLoadingMore = true
                                currentPage += 1
                                val next = if (onlineSearchQuery.isBlank()) {
                                    repo.searchSongsOnline("top", page = currentPage, perPage = 20)
                                } else {
                                    repo.searchSongsOnline(onlineSearchQuery, page = currentPage, perPage = 20)
                                }
                                if (next.isEmpty()) {
                                    canLoadMore = false
                                } else {
                                    onlineSongs = onlineSongs + next
                                }
                                isLoadingMore = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun OfflineTab(songs: List<Song>, onSongClick: (Song) -> Unit, onDeleteSong: (Song) -> Unit) {
    // Pull-to-refresh removed
    
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
            // Removed PullRefreshIndicator
        }
    } else {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            Column(
                Modifier.fillMaxSize()
            ) {
            // Search bar and clear button
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
                
                // Song count indicator
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
            
            // Removed PullRefreshIndicator
        }
    }
}

@Composable
fun OnlineTab(
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
                    "Search for songs to get results from JioSaavn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize()
        ) {
            // Search bar and download all button
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Styled search with better contrast and shape
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
                
                // Song count indicator
                if (!isSearching) {
                    Text(
                        text = "${filteredSongs.size} song${if (filteredSongs.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                
                // Download all button
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
                
                // Download progress indicator
                if (isLoadingMore) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        progress = { 0.5f }
                    )
                    Text(
                        text = "Loading more…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            // Suggestions dropdown like previous search: show close matches beneath search
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
                            isDownloaded = isSongDownloaded(song),
                            isDownloading = downloadingSongs.contains(song.id.orEmpty())
                        )
                    }
                    // Infinite scroll trigger
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

@Composable
fun OnlineSongItem(
    song: Song,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    isDownloaded: Boolean,
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
                    // Show progress indicator when downloading
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    
                    // Download button with proper state
                    IconButton(
                        onClick = onDownloadClick,
                        enabled = !isDownloaded && !isDownloading
                    ) {
                        Icon(
                            if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                            contentDescription = if (isDownloaded) "Already Downloaded" else "Download",
                            tint = if (isDownloaded) MaterialTheme.colorScheme.primary 
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