package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.content.Context
import coil.compose.AsyncImage
import com.example.DhoonHub.R
import com.example.DhoonHub.network.RetrofitProvider
import com.example.DhoonHub.network.api.Album
import com.example.DhoonHub.network.api.MusicApi
import com.example.DhoonHub.repository.AuthRepository
import com.example.DhoonHub.storage.SettingsStorage
import com.example.DhoonHub.ui.components.ErrorScreen
import com.example.DhoonHub.ui.components.LoadingScreen
import com.example.DhoonHub.ui.components.ShimmerAlbumCard
import com.example.DhoonHub.ui.components.EmptyScreen
import com.example.DhoonHub.viewmodel.MusicViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.DhoonHub.player.DhoonHubService
import androidx.compose.foundation.lazy.grid.rememberLazyGridState // Import for LazyGridState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    rootNav: NavController,
    musicViewModel: MusicViewModel // Receive the shared ViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authRepo = remember { AuthRepository(context) }
    val settings = remember { SettingsStorage.getInstance(context) }
    
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("English", "Hindi", "Bengali", "Punjabi", "Tamil", "Telugu")
    var selectedLanguage by remember { mutableStateOf(settings.getLanguage()) }

    val coroutineScope = rememberCoroutineScope()

    var albumSearchQuery by remember { mutableStateOf("") }

    val albumGridState = rememberLazyGridState() // State for infinite scrolling

    // Detect scroll to end for infinite scrolling
    LaunchedEffect(albumGridState) {
        snapshotFlow { albumGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= musicViewModel.albums.size - 1 && musicViewModel.canLoadMoreAlbums && !musicViewModel.isLoadingAlbums && !musicViewModel.isPaginatingAlbums) {
                    musicViewModel.loadMoreAlbums()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Home",
                        modifier = Modifier.clickable {
                            // Clear search query and results when "Home" is clicked
                            albumSearchQuery = ""  // Clear the search query
                            musicViewModel.clearAlbumSearchResults()  // Clear search results
                            musicViewModel.refreshAll()  // Refresh the albums
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* root */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { expanded = true }) { Text(selectedLanguage) }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            languages.forEach { lang ->
                                DropdownMenuItem(text = { Text(lang) }, onClick = {
                                    selectedLanguage = lang
                                    settings.setLanguage(lang)
                                    expanded = false
                                    // Force refresh when language changes
                                    musicViewModel.refreshAll()
                                })
                            }
                        }
                    }
                    TextButton(onClick = {
                        authRepo.logout()
                        rootNav.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }) { Text("Logout") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = albumSearchQuery,
                onValueChange = { query ->
                    albumSearchQuery = query
                    if (query.isBlank()) {
                        // Clear search results when query is empty
                        musicViewModel.clearAlbumSearchResults()
                    } else {
                        musicViewModel.searchAlbums(query)
                    }
                },
                label = { Text("Search Albums") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { musicViewModel.searchAlbums(albumSearchQuery) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )

            if (musicViewModel.isSearchingAlbums) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (musicViewModel.albumSearchError != null) {
                Text(
                    "Error: ${musicViewModel.albumSearchError}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (albumSearchQuery.isNotBlank() && musicViewModel.albumSearchResults.isEmpty()) {
                Text(
                    text = "No albums found for $albumSearchQuery",
                    modifier = Modifier.padding(16.dp)
                )
            } else if (musicViewModel.albumSearchResults.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(musicViewModel.albumSearchResults) { song ->
                        SongSearchCard(song = song, onClick = {
                            DhoonHubService.startPlayUrl(
                                context,
                                song.url,
                                title = song.title,
                                artist = song.artist,
                                artworkUrl = song.thumbnail
                            )
                            rootNav.navigate("player")
                        })
                    }
                }
            } else {
                // Original content for displaying albums
                when {
                    musicViewModel.isLoadingAlbums && musicViewModel.albums.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text(
                                "Good evening",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                contentPadding = PaddingValues(4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(6) { // Show 6 shimmer cards
                                    ShimmerAlbumCard()
                                }
                            }
                        }
                    }
                    
                    musicViewModel.albumsError != null && musicViewModel.albums.isEmpty() -> {
                        ErrorScreen(
                            message = musicViewModel.albumsError!!,
                            onRetry = {
                                coroutineScope.launch { musicViewModel.refreshAll() }
                            }
                        )
                    }
                    
                    musicViewModel.albums.isEmpty() && !musicViewModel.isLoadingAlbums -> {
                        EmptyScreen(
                            title = "No Music Found",
                            subtitle = "We couldn't find any albums. Try changing the language or check your connection."
                        )
                    }
                    
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Show loading indicator at top if refreshing
                            if (musicViewModel.isLoadingAlbums && musicViewModel.albums.isNotEmpty()) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            // Show error message if there's an error but we have cached data
                            if (musicViewModel.albumsError != null && musicViewModel.albums.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Failed to refresh: ${musicViewModel.albumsError}",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = {
                                                coroutineScope.launch { musicViewModel.refreshAll() }
                                            }
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Text(
                                "Good evening",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyVerticalGrid(
                                state = albumGridState, // Assign the state
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                contentPadding = PaddingValues(4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(musicViewModel.albums) { album ->
                                    AlbumCard(album = album, onClick = {
                                        rootNav.navigate("album/${album.name}")
                                    })
                                }
                                // Loading indicator for pagination
                                if (musicViewModel.isPaginatingAlbums) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit) {
    ElevatedCard(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            AsyncImage(
                model = album.songs.firstOrNull()?.thumbnail,
                contentDescription = album.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = painterResource(R.drawable.ic_music_note),
                error = painterResource(R.drawable.ic_music_note)
            )
            Text(album.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                album.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}



@Composable
fun SongSearchCard(song: com.example.DhoonHub.model.Song, onClick: () -> Unit) {
    ElevatedCard(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = song.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = painterResource(R.drawable.ic_music_note),
                error = painterResource(R.drawable.ic_music_note)
            )
            Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

fun getAlbumSongs(albumName: String, context: Context, onResult: (List<com.example.DhoonHub.model.Song>) -> Unit) {
    // This function is not ideal, consider moving to ViewModel
    val musicApi = RetrofitProvider.getMusicApi(context)
    
    // This is a simplistic implementation. In a real app, you'd use a proper coroutine scope.
    // For this example, we'll just launch a new one.
    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val response = musicApi.getAlbumSongs(albumName)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(response.songs)
            }
        } catch (e: Exception) {
            // Handle error
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(emptyList())
            }
        }
    }
}