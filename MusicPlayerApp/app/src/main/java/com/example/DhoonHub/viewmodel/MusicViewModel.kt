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
import com.example.DhoonHub.network.api.TrendingArtist
import com.example.DhoonHub.network.api.MusicApi
import com.example.DhoonHub.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicViewModel(private val context: Context) : ViewModel() {

    private var searchJob: Job? = null
    
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
    var currentOnlineSongPage by mutableStateOf(1)
        private set
    var canLoadMoreOnlineSongs by mutableStateOf(true)
        private set
    var isPaginatingOnlineSongs by mutableStateOf(false)
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

    var searchQuery by mutableStateOf("")
        private set

    // Album search results
    var albumSearchResults by mutableStateOf<List<Song>>(emptyList())
        private set
    var isSearchingAlbums by mutableStateOf(false)
        private set
    var albumSearchError by mutableStateOf<String?>(null)
        private set

    // Artist search results
    var artistSearchResults by mutableStateOf<List<TrendingArtist>>(emptyList())
        private set
    var isSearchingArtists by mutableStateOf(false)
        private set
    var artistSearchError by mutableStateOf<String?>(null)
        private set

    // Popular artists state
    var popularArtists by mutableStateOf<List<String>>(emptyList())
        private set
    var isLoadingPopularArtists by mutableStateOf(false)
        private set
    var popularArtistsError by mutableStateOf<String?>(null)
        private set
    var currentArtistPage by mutableStateOf(1)
        private set
    var canLoadMoreArtists by mutableStateOf(true)
        private set

    // Artist songs state
    var artistSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var isLoadingArtistSongs by mutableStateOf(false)
        private set
    var artistSongsError by mutableStateOf<String?>(null)
        private set
    
    // Album details
    private val albumSongsCache = mutableMapOf<String, List<Song>>()
    private val artistDetailsCache = mutableMapOf<String, TrendingArtist>()
    
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
        loadOnlineSongs(page = currentOnlineSongPage)
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
                    albums = if (page == 1) fetchedAlbums else albums + fetchedAlbums // Append for pagination
                    currentAlbumPage = page
                    canLoadMoreAlbums = fetchedAlbums.size == perPage // Update canLoadMoreAlbums
                } else {
                    canLoadMoreAlbums = false // No more albums to load
                }

                // Handle creating albums from songs if no albums were returned (existing logic)
                if (albums.isEmpty() && page == 1) { // Only do this fallback for the first page if no albums
                    val songs = runCatching { musicApi.getSongs(page = 1).songs }
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
        isLoadingOfflineSongs = true
        
        viewModelScope.launch {
            try {
                val offlineFiles = musicRepository.getOfflineSongsWithMetadata()
                offlineSongs = offlineFiles
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoadingOfflineSongs = false
            }
        }
    }
    
    fun loadOnlineSongs(page: Int = 1) {
        if (isLoadingOnlineSongs || isPaginatingOnlineSongs || !canLoadMoreOnlineSongs) return

        if (page == 1) {
            isLoadingOnlineSongs = true
            onlineSongsError = null
        } else {
            isPaginatingOnlineSongs = true
        }

        viewModelScope.launch {
            try {
                val fetchedSongs = musicRepository.getSongsOnline(page)
                if (fetchedSongs.isNotEmpty()) {
                    onlineSongs = if (page == 1) fetchedSongs else onlineSongs + fetchedSongs
                    currentOnlineSongPage = page
                    canLoadMoreOnlineSongs = fetchedSongs.size == 10 // Assuming 10 per page
                } else {
                    canLoadMoreOnlineSongs = false
                }
            } catch (e: Exception) {
                onlineSongsError = "Failed to load online songs: ${e.message}"
                canLoadMoreOnlineSongs = false
            } finally {
                isLoadingOnlineSongs = false
                isPaginatingOnlineSongs = false
            }
        }
    }

    fun loadMoreOnlineSongs() {
        if (canLoadMoreOnlineSongs && !isLoadingOnlineSongs && !isPaginatingOnlineSongs) {
            loadOnlineSongs(page = currentOnlineSongPage + 1)
        }
    }
    
    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500L)
            searchSongs(query)
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

    fun searchArtists(artistName: String) {
        if (artistName.isBlank()) {
            artistSearchResults = emptyList()
            return
        }

        isSearchingArtists = true
        artistSearchError = null

        viewModelScope.launch {
            try {
                val artist = musicRepository.searchArtistByName(artistName)
                artistSearchResults = if (artist != null) listOf(artist) else emptyList()
            } catch (e: Exception) {
                artistSearchError = "Artist search failed: ${e.message}"
            } finally {
                isSearchingArtists = false
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
    
    
    
    // Add this method to your MusicViewModel class
    fun clearAlbumSearchResults() {
        albumSearchResults = emptyList()
    }

    fun clearArtistSearchResults() {
        artistSearchResults = emptyList()
    }

    fun clearArtistSongs() {
        artistSongs = emptyList()
        isLoadingArtistSongs = false
        artistSongsError = null
    }
    
    fun clearArtistCache() {
        artistDetailsCache.clear()
    }
    
    suspend fun clearAlbumsCache(): String {
        return try {
            val response = musicApi.clearAlbumsCache()
            response.message
        } catch (e: Exception) {
            "Failed to clear cache: ${e.message}"
        }
    }

    fun loadPopularArtists(page: Int = 1, limit: Int = 20) {
        if (isLoadingPopularArtists || !canLoadMoreArtists) return

        isLoadingPopularArtists = true
        popularArtistsError = null

        viewModelScope.launch {
            try {
                val fetchedArtists = musicRepository.getPopularArtists(page, limit)
                if (fetchedArtists.isNotEmpty()) {
                    popularArtists = if (page == 1) fetchedArtists else popularArtists + fetchedArtists
                    currentArtistPage = page
                    canLoadMoreArtists = fetchedArtists.size == limit
                } else {
                    canLoadMoreArtists = false
                }
            } catch (e: Exception) {
                popularArtistsError = "Failed to load popular artists: ${e.message}"
            } finally {
                isLoadingPopularArtists = false
            }
        }
    }

    fun loadMoreArtists() {
        if (canLoadMoreArtists && !isLoadingPopularArtists) {
            loadPopularArtists(page = currentArtistPage + 1)
        }
    }

    suspend fun getArtistDetails(artistName: String): TrendingArtist? {
        artistDetailsCache[artistName]?.let { return it }
        val artist = musicRepository.getArtistDetails(artistName)
        artist?.let { artistDetailsCache[artistName] = it }
        return artist
    }

    fun loadArtistSongs(artistName: String) {
        isLoadingArtistSongs = true
        artistSongsError = null
        viewModelScope.launch {
            try {
                artistSongs = musicRepository.getArtistSongs(artistName)
            } catch (e: Exception) {
                artistSongsError = "Failed to load artist songs: ${e.message}"
            } finally {
                isLoadingArtistSongs = false
            }
        }
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