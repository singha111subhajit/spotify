package com.example.DhoonHub.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.DhoonHub.model.Song
import com.example.DhoonHub.network.RetrofitProvider
import com.example.DhoonHub.network.api.Album
import com.example.DhoonHub.network.api.MusicApi
import com.example.DhoonHub.repository.MusicRepository
import kotlinx.coroutines.launch

class MusicViewModel(private val context: Context) : ViewModel() {
    
    // Albums state
    var albums by mutableStateOf<List<Album>>(emptyList())
        private set
    var isLoadingAlbums by mutableStateOf(false)
        private set
    var albumsError by mutableStateOf<String?>(null)
        private set
    
    // Online songs state
    var onlineSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var isLoadingOnlineSongs by mutableStateOf(false)
        private set
    var onlineSongsError by mutableStateOf<String?>(null)
        private set
    
    // Offline songs state
    var offlineSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var isLoadingOfflineSongs by mutableStateOf(false)
        private set
    
    // Search results
    var searchResults by mutableStateOf<List<Song>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set

    // Album search results
    var albumSearchResults by mutableStateOf<List<Song>>(emptyList())
        private set
    var isSearchingAlbums by mutableStateOf(false)
        private set
    var albumSearchError by mutableStateOf<String?>(null)
        private set
    
    // Album details
    private val albumSongsCache = mutableMapOf<String, List<Song>>()
    
    private val musicApi = RetrofitProvider.getRetrofit(context).create(MusicApi::class.java)
    val musicRepository = MusicRepository(context)
    
    // Pagination state for albums
    var currentAlbumPage by mutableStateOf(1)
        private set
    var canLoadMoreAlbums by mutableStateOf(true)
        private set
    var isPaginatingAlbums by mutableStateOf(false) // New state for pagination loading indicator
        private set

    init {
        // Load initial data
        loadAlbums(page = currentAlbumPage) // Call with initial page
        loadOfflineSongs()
    }
    
    fun loadAlbums(page: Int = 1, perPage: Int = 20) { // Add parameters
        if (isLoadingAlbums || isPaginatingAlbums || !canLoadMoreAlbums) return // Prevent multiple loads

        if (page == 1) { // Only show full loading indicator for the first page
            isLoadingAlbums = true
            albumsError = null
        } else {
            isPaginatingAlbums = true // Show pagination loading indicator for subsequent pages
        }

        viewModelScope.launch {
            try {
                val fetchedAlbums = runCatching { musicApi.getAlbums(page = page, perPage = perPage).albums } // Pass page and perPage
                    .getOrElse {
                        albumsError = "Failed to fetch albums: ${it.message}"
                        emptyList()
                    }

                if (fetchedAlbums.isNotEmpty()) {
                    albums = if (page == 1) fetchedAlbums.shuffled() else albums + fetchedAlbums.shuffled() // Append for pagination
                    currentAlbumPage = page
                    canLoadMoreAlbums = fetchedAlbums.size == perPage // Update canLoadMoreAlbums
                } else {
                    canLoadMoreAlbums = false // No more albums to load
                }

                // Handle creating albums from songs if no albums were returned (existing logic)
                if (albums.isEmpty() && page == 1) { // Only do this fallback for the first page if no albums
                    val songs = runCatching { musicApi.getSongs().songs }
                        .getOrElse {
                            albumsError = "Failed to fetch songs: ${it.message}"
                            emptyList()
                        }

                    if (songs.isNotEmpty()) {
                        val grouped = songs.groupBy { it.album ?: "Unknown Album" }
                        albums = grouped.entries.mapIndexed { index, entry ->
                            val name = entry.key
                            val groupSongs = entry.value
                            val first = groupSongs.first()
                            Album(
                                id = "album-$index",
                                name = name,
                                artist = first.artist,
                                song_count = groupSongs.size,
                                songs = groupSongs
                            )
                        }.shuffled()
                        canLoadMoreAlbums = false // No pagination for generated albums
                    }
                }

            } catch (e: Exception) {
                albumsError = "Error loading albums: ${e.message}"
                canLoadMoreAlbums = false // Stop loading on error
            } finally {
                isLoadingAlbums = false
                isPaginatingAlbums = false
            }
        }
    }

    fun loadMoreAlbums() {
        if (canLoadMoreAlbums && !isLoadingAlbums && !isPaginatingAlbums) {
            loadAlbums(page = currentAlbumPage + 1)
        }
    }
    
    fun loadOfflineSongs() {
        if (offlineSongs.isNotEmpty()) return // Don't reload if we already have data
        
        isLoadingOfflineSongs = true
        
        viewModelScope.launch {
            try {
                val offlineFiles = musicRepository.getOfflineSongs()
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
                    )
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoadingOfflineSongs = false
            }
        }
    }
    
    fun loadOnlineSongs() {
        if (onlineSongs.isNotEmpty()) return // Don't reload if we already have data
        
        isLoadingOnlineSongs = true
        onlineSongsError = null
        
        viewModelScope.launch {
            try {
                onlineSongs = musicRepository.getSongsOnline()
            } catch (e: Exception) {
                onlineSongsError = "Failed to load online songs: ${e.message}"
            } finally {
                isLoadingOnlineSongs = false
            }
        }
    }
    
    fun searchSongs(query: String, page: Int = 1, perPage: Int = 20) {
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }
        
        isSearching = true
        searchError = null
        
        viewModelScope.launch {
            try {
                searchResults = musicRepository.searchSongsOnline(query, page, perPage)
            } catch (e: Exception) {
                searchError = "Search failed: ${e.message}"
            } finally {
                isSearching = false
            }
        }
    }

    fun searchAlbums(albumName: String) {
        if (albumName.isBlank()) {
            albumSearchResults = emptyList()
            return
        }

        isSearchingAlbums = true
        albumSearchError = null

        viewModelScope.launch {
            try {
                albumSearchResults = musicRepository.getAlbumSongs(albumName)
            } catch (e: Exception) {
                albumSearchError = "Album search failed: ${e.message}"
            } finally {
                isSearchingAlbums = false
            }
        }
    }
    
    suspend fun getAlbumSongs(albumName: String): List<Song> {
        // 1. Return cached songs if available.
        albumSongsCache[albumName]?.let { if (it.isNotEmpty()) return it }

        // 2. Check the main 'albums' state, which should be populated by `loadAlbums`.
        val albumFromState = albums.find { it.name == albumName }
        if (albumFromState != null && albumFromState.songs.isNotEmpty()) {
            albumSongsCache[albumName] = albumFromState.songs
            return albumFromState.songs
        }

        // 3. Fallback: If songs are not in the state (e.g., `loadAlbums` hasn't completed
        // or returned albums without song lists), fetch albums directly.
        // This fetch is self-contained and does not modify the main `albums` state,
        // preventing side effects.
        return try {
            val fetchedAlbums = musicApi.getAlbums().albums
            val targetAlbum = fetchedAlbums.find { it.name == albumName }
            val songs = targetAlbum?.songs ?: emptyList()

            if (songs.isNotEmpty()) {
                albumSongsCache[albumName] = songs
            }
            songs
        } catch (e: Exception) {
            albumsError = "Failed to fetch songs for album '$albumName': ${e.message}"
            emptyList()
        }
    }
    
    fun refreshAll() {
        // Force refresh all data
        albums = emptyList()
        onlineSongs = emptyList()
        offlineSongs = emptyList()
        albumSongsCache.clear()
        currentAlbumPage = 1 // Reset pagination
        canLoadMoreAlbums = true // Reset pagination
        isPaginatingAlbums = false // Reset pagination

        loadAlbums(page = currentAlbumPage) // Load first page
        loadOnlineSongs()
        loadOfflineSongs()
    }
    
    // Provide a public method to access musicApi
    fun getMusicApi(): MusicApi {
        return musicApi
    }

    // Provide a public method to access albumSongsCache
    fun cacheAlbumSongs(albumName: String, songs: List<Song>) {
        albumSongsCache[albumName] = songs
    }
    
    // Add this method to your MusicViewModel class
    fun clearAlbumSearchResults() {
        albumSearchResults = emptyList()
    }
    
    // Factory to create the ViewModel with context
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                return MusicViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}