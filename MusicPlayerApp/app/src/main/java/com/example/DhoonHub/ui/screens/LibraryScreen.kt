package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.DhoonHub.model.Song
import com.example.DhoonHub.player.DhoonHubService
import com.example.DhoonHub.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun refreshOfflineSongs() {
        coroutineScope.launch {
            offlineSongs = repo.getOfflineSongsWithMetadata()
            downloadedSongIds = offlineSongs.mapNotNull { it.id }.toSet()
        }
    }
    
    fun downloadSong(song: Song) {
        if (downloadingSongs.contains(song.id.orEmpty())) return
        
        val songId = song.id.orEmpty()
        downloadingSongs = downloadingSongs + songId
        
        coroutineScope.launch {
            try {
                val success = repo.downloadSong(song)
                if (success) {
                    refreshOfflineSongs()
                    downloadedSongIds = downloadedSongIds + songId
                }
            } finally {
                downloadingSongs = downloadingSongs - songId
            }
        }
    }
    
    fun isSongDownloaded(song: Song): Boolean {
        return repo.isSongDownloaded(song)
    }
    
    LaunchedEffect(Unit) {
        refreshOfflineSongs()
    }

    LaunchedEffect(Unit) {
        refreshOfflineSongs()
    }

    LaunchedEffect(currentTab, onlineSearchQuery) {
        if (currentTab == 1) {
            error = null
            if (onlineSearchQuery.isBlank()) {
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

            when (currentTab) {
                0 -> OfflineScreen(
                    songs = offlineSongs,
                    onSongClick = { song ->
                        DhoonHubService.startPlayFile(context, song)
                        rootNav.navigate("player")
                    },
                    onDeleteSong = { song ->
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                repo.deleteDownloadedSong(song)
                            }
                            refreshOfflineSongs()
                        }
                    }
                )
                1 -> OnlineScreen(
                    songs = onlineSongs,
                    isLoading = isLoading,
                    error = error,
                    searchQuery = onlineSearchQuery,
                    isSearching = isSearching,
                    onSearchQueryChange = { q -> onlineSearchQuery = q },
                    onSongClick = { song ->
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
