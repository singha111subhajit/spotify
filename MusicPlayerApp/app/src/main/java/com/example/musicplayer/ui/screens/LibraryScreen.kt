package com.example.musicplayer.ui.screens

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
import com.example.musicplayer.model.Song
import com.example.musicplayer.player.MusicPlayerService
import com.example.musicplayer.repository.MusicRepository
import com.example.musicplayer.storage.TokenStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(rootNav: NavController) {
    val context = LocalContext.current
    val repo = remember { MusicRepository(context) }
    val tokenStorage = remember { TokenStorage.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var currentTab by remember { mutableStateOf(0) }
    var offlineSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var onlineSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadingSongs by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Function to refresh offline songs
    fun refreshOfflineSongs() {
        val offlineFiles = repo.getOfflineSongs()
        offlineSongs = offlineFiles.map { file ->
            // Parse filename to extract song info
            val filename = file.nameWithoutExtension
            val parts = filename.split("-")
            val artist = if (parts.size > 1) parts[0] else "Unknown Artist"
            val title = if (parts.size > 2) parts[1] else filename
            Song(
                id = file.absolutePath,
                title = title,
                artist = artist,
                url = file.absolutePath,
                thumbnail = null,
                album = null
            )
        }.sortedBy { it.title.lowercase() }
    }

    // Load offline songs
    LaunchedEffect(Unit) {
        refreshOfflineSongs()
    }

    // Load online songs when online tab is selected
    LaunchedEffect(currentTab) {
        if (currentTab == 1 && onlineSongs.isEmpty()) {
            isLoading = true
            error = null
            try {
                onlineSongs = repo.getSongsOnline()
            } catch (e: Exception) {
                error = "Failed to load online songs: ${e.message}"
            } finally {
                isLoading = false
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
                        MusicPlayerService.startPlayFile(context, song.url)
                        rootNav.navigate("player")
                    },
                    onRefresh = { refreshOfflineSongs() },
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
                1 ->                 OnlineTab(
                    songs = onlineSongs,
                    isLoading = isLoading,
                    error = error,
                    onSongClick = { song ->
                        // For online songs, we need to download first
                        coroutineScope.launch {
                            val downloadedFile = repo.downloadSong(song)
                            downloadedFile?.let {
                                MusicPlayerService.startPlayFile(context, it.absolutePath)
                                rootNav.navigate("player")
                            }
                        }
                    },
                    onDownloadClick = { song ->
                        coroutineScope.launch {
                            downloadingSongs = downloadingSongs + song.id.orEmpty()
                            try {
                                repo.downloadSong(song)
                                // Refresh offline songs list
                                refreshOfflineSongs()
                            } finally {
                                downloadingSongs = downloadingSongs - song.id.orEmpty()
                            }
                        }
                    },
                    isSongDownloaded = { song -> repo.isSongDownloaded(song) },
                    downloadingSongs = downloadingSongs,
                    onRefreshOffline = { refreshOfflineSongs() }
                )
            }
        }
    }
}

@Composable
fun OfflineTab(songs: List<Song>, onSongClick: (Song) -> Unit, onRefresh: () -> Unit, onDeleteSong: (Song) -> Unit) {
    // Pull-to-refresh removed; call onRefresh via a button if needed
    
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
    onSongClick: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    isSongDownloaded: (Song) -> Boolean,
    downloadingSongs: Set<String>,
    onRefreshOffline: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSongs = if (searchQuery.isEmpty()) {
        songs
    } else {
        songs.filter { song ->
            song.title.contains(searchQuery, ignoreCase = true) ||
            song.artist.contains(searchQuery, ignoreCase = true) ||
            (song.album?.contains(searchQuery, ignoreCase = true) == true)
        }
    }
    
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
            Text(
                "No songs available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search online songs...") },
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
                if (downloadingSongs.isNotEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        progress = 0.5f // This could be enhanced with actual progress tracking
                    )
                    Text(
                        text = "Downloading ${downloadingSongs.size} song${if (downloadingSongs.size != 1) "s" else ""}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
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
                        OnlineSongItem(
                            song = song,
                            onPlayClick = { onSongClick(song) },
                            onDownloadClick = { onDownloadClick(song) },
                            isDownloaded = isSongDownloaded(song),
                            isDownloading = downloadingSongs.contains(song.id.orEmpty())
                        )
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
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                song.album?.let { album ->
                    Text(
                        text = album,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDownloadClick,
                    enabled = !isDownloaded && !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                            contentDescription = if (isDownloaded) "Already Downloaded" else "Download",
                            tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
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
