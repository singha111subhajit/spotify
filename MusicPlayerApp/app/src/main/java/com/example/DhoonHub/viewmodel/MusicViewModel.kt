
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
    
    // Album details
    private val albumSongsCache = mutableMapOf<String, List<Song>>()
    
    private val musicApi = RetrofitProvider.getRetrofit(context).create(MusicApi::class.java)
    private val musicRepository = MusicRepository(context)
    
    init {
        // Load initial data
        loadAlbums()
        loadOfflineSongs()
    }
    
    fun loadAlbums() {
        if (albums.isNotEmpty()) return // Don't reload if we already have data
        
        isLoadingAlbums = true
        albumsError = null
        
        viewModelScope.launch {
            try {
                val fetchedAlbums = runCatching { musicApi.getAlbums().albums }
                    .getOrElse {
                        albumsError = "Failed to fetch albums: ${it.message}"
                        emptyList()
                    }
                
                if (fetchedAlbums.isNotEmpty()) {
                    albums = fetchedAlbums.shuffled()
                } else {
                    // Try to create albums from songs if no albums were returned
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
                    }
                }
            } catch (e: Exception) {
                albumsError = "Error loading albums: ${e.message}"
            } finally {
                isLoadingAlbums = false
            }
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
    
    fun getAlbumSongs(albumName: String): List<Song> {
        // Return cached songs if available
        albumSongsCache[albumName]?.let { return it }
        
        // Find album in our albums list
        val album = albums.find { it.name == albumName }
        val songs = album?.songs ?: emptyList()
        
        // Cache the result
        if (songs.isNotEmpty()) {
            albumSongsCache[albumName] = songs
        }
        
        return songs
    }
    
    fun refreshAll() {
        // Force refresh all data
        albums = emptyList()
        onlineSongs = emptyList()
        offlineSongs = emptyList()
        albumSongsCache.clear()
        
        loadAlbums()
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
