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
import com.example.DhoonHub.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    rootNav: NavController,
    musicViewModel: MusicViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var currentTab by remember { mutableStateOf(0) }
    var downloadingSongs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSongIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val offlineSongs by musicViewModel.offlineSongs.collectAsState()

    fun refreshOfflineSongs() {
        musicViewModel.loadOfflineSongs()
    }
    
    fun downloadSong(song: Song) {
        if (downloadingSongs.contains(song.id.orEmpty())) return
        
        val songId = song.id.orEmpty()
        downloadingSongs = downloadingSongs + songId
        
        coroutineScope.launch {
            try {
                val success = musicViewModel.musicRepository.downloadSong(song)
                if (success) {
                    refreshOfflineSongs()
                }
            } finally {
                downloadingSongs = downloadingSongs - songId
            }
        }
    }
    
    fun isSongDownloaded(song: Song): Boolean {
        return musicViewModel.musicRepository.isSongDownloaded(song)
    }

    fun onToggleSelection(songId: String) {
        selectedSongIds = if (selectedSongIds.contains(songId)) {
            selectedSongIds - songId
        } else {
            selectedSongIds + songId
        }
    }
    
    LaunchedEffect(Unit) {
        refreshOfflineSongs()
    }

    LaunchedEffect(currentTab) {
        if (currentTab == 1 && musicViewModel.onlineSongs.isEmpty()) {
            musicViewModel.loadOnlineSongs()
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
                    text = { Text("Online") }, // Size is now managed inside OnlineScreen
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "Online") }
                )
            }

            when (currentTab) {
                0 -> OfflineScreen(musicViewModel = musicViewModel, rootNav = rootNav)
                1 -> OnlineScreen(
                    onlineSongs = musicViewModel.onlineSongs,
                    searchResults = musicViewModel.searchResults,
                    isLoading = musicViewModel.isLoadingOnlineSongs,
                    error = musicViewModel.searchError,
                    searchQuery = musicViewModel.searchQuery,
                    isSearching = musicViewModel.isSearching,
                    onSearchQueryChange = { musicViewModel.onSearchQueryChanged(it) },
                    onSongClick = { song ->
                        val songsToPlay = if (musicViewModel.searchQuery.isBlank()) {
                            musicViewModel.onlineSongs
                        } else {
                            musicViewModel.searchResults
                        }
                        val startIndex = songsToPlay.indexOf(song)
                        DhoonHubService.startPlayUrl(
                            context,
                            songs = songsToPlay,
                            startIndex = startIndex
                        )
                        rootNav.navigate("player")
                    },
                    onDownloadClick = { song ->
                        downloadSong(song)
                    },
                    isSongDownloaded = { song -> isSongDownloaded(song) },
                    downloadingSongs = downloadingSongs,
                    selectedSongIds = selectedSongIds,
                    onToggleSelection = ::onToggleSelection,
                    loadMoreOnlineSongs = { musicViewModel.loadMoreOnlineSongs() },
                    isPaginatingOnlineSongs = musicViewModel.isPaginatingOnlineSongs
                )
            }
        }
    }
}