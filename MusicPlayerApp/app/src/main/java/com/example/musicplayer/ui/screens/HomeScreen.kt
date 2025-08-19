package com.example.DhoonHub.ui.screens

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
import com.example.DhoonHub.R
import com.example.DhoonHub.network.RetrofitProvider
import com.example.DhoonHub.network.api.Album
import com.example.DhoonHub.network.api.MusicApi
import com.example.DhoonHub.repository.AuthRepository
import com.example.DhoonHub.storage.SettingsStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(rootNav: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val retrofit = remember { RetrofitProvider.getRetrofit(context) }
    val musicApi = remember { retrofit.create(MusicApi::class.java) }
    val authRepo = remember { AuthRepository(context) }
    val settings = remember { SettingsStorage.getInstance(context) }

    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("English", "Hindi", "Bengali", "Punjabi", "Tamil", "Telugu")
    var selectedLanguage by remember { mutableStateOf(settings.getLanguage()) }

    val coroutineScope = rememberCoroutineScope()

    suspend fun load() {
        isLoading = true
        error = null

        val songs = runCatching { musicApi.getSongs().songs }
            .getOrElse {
                error = "Failed to fetch songs"
                emptyList()
            }

        var fetchedAlbums = runCatching { musicApi.getAlbums().albums }
            .getOrElse {
                error = "Failed to fetch albums"
                emptyList()
            }

        if (fetchedAlbums.isEmpty() && songs.isNotEmpty()) {
            val grouped = songs.groupBy { it.album ?: "Unknown Album" }
            fetchedAlbums = grouped.entries.mapIndexed { index, entry ->
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
            }
        }

        albums = fetchedAlbums.shuffled()
        isLoading = false
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
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
                                    coroutineScope.launch { load() }
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
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error != null) Text("Error: $error", color = MaterialTheme.colorScheme.error)

            Text(
                "Good evening",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(albums) { album ->
                    AlbumCard(album = album, onClick = {
                        rootNav.navigate("album/${album.name}")
                    })
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
