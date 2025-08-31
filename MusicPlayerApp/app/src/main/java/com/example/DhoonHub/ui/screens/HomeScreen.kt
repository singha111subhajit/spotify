
package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.* // Import all runtime components
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController
import android.content.Context
import coil.compose.AsyncImage
import com.example.DhoonHub.R
import com.example.DhoonHub.network.api.Album
import com.example.DhoonHub.network.api.TrendingArtist
import com.example.DhoonHub.ui.components.ErrorScreen
import com.example.DhoonHub.ui.components.LoadingScreen
import com.example.DhoonHub.ui.components.ShimmerAlbumCard
import com.example.DhoonHub.ui.components.EmptyScreen
import com.example.DhoonHub.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import com.example.DhoonHub.player.DhoonHubService
import androidx.compose.foundation.lazy.grid.rememberLazyGridState // Import for LazyGridState
import androidx.compose.runtime.snapshotFlow // Import for snapshotFlow
import com.example.DhoonHub.ui.components.AlbumCard // Added import
import com.example.DhoonHub.ui.components.SongSearchCard // Added import
import com.example.DhoonHub.ui.components.ArtistCard // Added import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    rootNavController: NavController,
    musicViewModel: MusicViewModel // Receive the shared ViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var albumSearchQuery by remember { mutableStateOf("") }
    var artistSearchQuery by remember { mutableStateOf("") }

    val albumGridState = rememberLazyGridState() // State for infinite scrolling

    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("Albums") }
    val types = listOf("Albums", "Artists")

    // Detect scroll to end for infinite scrolling
    LaunchedEffect(albumGridState) {
        snapshotFlow { albumGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                println("HomeScreen: lastIndex is $lastIndex, albums.size is ${musicViewModel.albums.size}")
                if (lastIndex != null && lastIndex >= musicViewModel.albums.size - 1 && musicViewModel.canLoadMoreAlbums && !musicViewModel.isLoadingAlbums && !musicViewModel.isPaginatingAlbums) {
                    android.util.Log.d("DhoonHub", "HomeScreen: Loading more albums.")
                    musicViewModel.loadMoreAlbums()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = if (selectedType == "Albums") albumSearchQuery else artistSearchQuery,
            onValueChange = { query ->
                if (selectedType == "Albums") {
                    albumSearchQuery = query
                    if (query.isBlank()) {
                        musicViewModel.clearAlbumSearchResults()
                    } else {
                        musicViewModel.searchAlbums(query)
                    }
                } else {
                    artistSearchQuery = query
                    if (query.isBlank()) {
                        musicViewModel.clearArtistSearchResults()
                    } else {
                        musicViewModel.searchArtists(query)
                    }
                }
            },
            label = { Text(if (selectedType == "Albums") "Search Albums" else "Search Artists") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    if (selectedType == "Albums") {
                        musicViewModel.searchAlbums(albumSearchQuery)
                    } else {
                        musicViewModel.searchArtists(artistSearchQuery)
                    }
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                types.forEach { type ->
                    DropdownMenuItem(text = { Text(type) }, onClick = { 
                        selectedType = type
                        expanded = false
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedType == "Albums") {
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
                                musicViewModel.albumSearchResults,
                                musicViewModel.albumSearchResults.indexOf(song)
                            )
                            rootNavController.navigate("player")
                        })
                    }
                }
            } else {
                // Original content for displaying albums
                when (selectedType) {
                    "Albums" -> {
                        when {
                            musicViewModel.isLoadingAlbums && musicViewModel.albums.isEmpty() -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 140.dp),
                                    contentPadding = PaddingValues(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(musicViewModel.popularArtists) { artistName ->
                                        ArtistCard(artistName = artistName, musicViewModel = musicViewModel, onClick = {
                                            navController.navigate("artist/${artistName}")
                                        })
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
                                Column {
                                    if (musicViewModel.isLoadingAlbums && musicViewModel.albums.isNotEmpty()) {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
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
                                                navController.navigate("album/${album.name}")
                                            })
                                        }
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
                    "Artists" -> {
                        LaunchedEffect(Unit) {
                            if (musicViewModel.popularArtists.isEmpty()) {
                                musicViewModel.loadPopularArtists()
                            }
                        }
                        LaunchedEffect(musicViewModel.popularArtists, musicViewModel.isLoadingPopularArtists, musicViewModel.popularArtistsError) {
                            android.util.Log.d("DhoonHub", "Popular Artists State:")
                            android.util.Log.d("DhoonHub", "  Artists: ${musicViewModel.popularArtists.size}")
                            android.util.Log.d("DhoonHub", "  Loading: ${musicViewModel.isLoadingPopularArtists}")
                            android.util.Log.d("DhoonHub", "  Error: ${musicViewModel.popularArtistsError}")
                        }
                        val artistGridState = rememberLazyGridState()
                        LaunchedEffect(artistGridState) {
                            snapshotFlow { artistGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                                .collect { lastIndex ->
                                    if (lastIndex != null && lastIndex >= musicViewModel.popularArtists.size - 1 && musicViewModel.canLoadMoreArtists && !musicViewModel.isLoadingPopularArtists) {
                                        musicViewModel.loadMoreArtists()
                                    }
                                }
                        }
                        when {
                            musicViewModel.isLoadingPopularArtists && musicViewModel.popularArtists.isEmpty() -> {
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
                            musicViewModel.popularArtistsError != null -> {
                                ErrorScreen(
                                    message = musicViewModel.popularArtistsError!!,
                                    onRetry = { musicViewModel.loadPopularArtists() }
                                )
                            }
                            musicViewModel.popularArtists.isEmpty() && !musicViewModel.isLoadingPopularArtists -> {
                                EmptyScreen(
                                    title = "No Artists Found",
                                    subtitle = "We couldn't find any popular artists. Try again later."
                                )
                            }
                            else -> {
                                LazyVerticalGrid(
                                    state = artistGridState,
                                    columns = GridCells.Adaptive(minSize = 140.dp),
                                    contentPadding = PaddingValues(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(musicViewModel.popularArtists) { artistName ->
                                        ArtistCard(artistName = artistName, musicViewModel = musicViewModel, onClick = {
                                            navController.navigate("artist/${artistName}")
                                        })
                                    }
                                    if (musicViewModel.isLoadingPopularArtists) {
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
        } else if (selectedType == "Artists") {
            LaunchedEffect(Unit) {
                if (musicViewModel.popularArtists.isEmpty()) {
                    musicViewModel.loadPopularArtists()
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(musicViewModel.popularArtists) { artistName ->
                    ArtistCard(artistName = artistName, musicViewModel = musicViewModel, onClick = {
                        navController.navigate("artist/${artistName}")
                    })
                }
            }
        }
    }
}
