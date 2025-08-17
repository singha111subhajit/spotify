package com.example.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicplayer.R
import com.example.musicplayer.model.Song
import com.example.musicplayer.network.RetrofitProvider
import com.example.musicplayer.network.api.Album
import com.example.musicplayer.network.api.MusicApi
import com.example.musicplayer.repository.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(rootNav: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val retrofit = remember { RetrofitProvider.getRetrofit(context) }
    val musicApi = remember { retrofit.create(MusicApi::class.java) }
    val authRepo = remember { AuthRepository(context) }

    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var featured by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        val songs = runCatching { musicApi.getSongs().songs }.getOrDefault(emptyList())
        var fetchedAlbums = runCatching { musicApi.getAlbums().albums }.getOrDefault(emptyList())
        if (fetchedAlbums.isEmpty() && songs.isNotEmpty()) {
            // Fallback: group songs by album name
            val grouped = songs.groupBy { it.thumbnail to (it.title + it.artist + (it.id ?: "")) }
            fetchedAlbums = songs.groupBy { it.title + it.artist }.mapIndexed { index, entry ->
                val first = entry.value.first()
                Album(id = "album-$index", name = first.album ?: (first.title), artist = first.artist, song_count = entry.value.size, songs = entry.value)
            }
        }
        albums = fetchedAlbums.shuffled()
        featured = songs.take(6)
        isLoading = false
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Home") },
            navigationIcon = {
                IconButton(onClick = { /* root screen, do nothing or open drawer */ }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                TextButton(onClick = {
                    authRepo.logout()
                    rootNav.navigate("login") { popUpTo("main") { inclusive = true } }
                }) { Text("Logout") }
            }
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error != null) Text("Error: ${'$'}error", color = MaterialTheme.colorScheme.error)

            Text("Good evening", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 140.dp), contentPadding = PaddingValues(4.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(albums) { album ->
                    AlbumCard(album = album, onClick = {
                        rootNav.navigate("album/${album.name}")
                    })
                }
            }

            if (featured.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Featured Songs", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                featured.forEach { song ->
                    ListItem(
                        leadingContent = {
                            AsyncImage(
                                model = song.thumbnail,
                                contentDescription = song.title,
                                placeholder = painterResource(R.drawable.ic_music_note),
                                error = painterResource(R.drawable.ic_music_note)
                            )
                        },
                        headlineContent = { Text(song.title) },
                        supportingContent = { Text(song.artist) },
                        trailingContent = { TextButton(onClick = {
                            com.example.musicplayer.player.MusicPlayerService.startPlayUrl(context, song.url, title = song.title, artist = song.artist, artworkUrl = song.thumbnail)
                            rootNav.navigate("player")
                        }) { Text("Play") } }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.Start) {
            AsyncImage(
                model = album.songs.firstOrNull()?.thumbnail,
                contentDescription = album.name,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = painterResource(R.drawable.ic_music_note),
                error = painterResource(R.drawable.ic_music_note)
            )
            Text(album.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}