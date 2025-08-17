package com.example.musicplayer.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicplayer.network.RetrofitProvider
import com.example.musicplayer.network.api.Album
import com.example.musicplayer.network.api.MusicApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(rootNav: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val retrofit = remember { RetrofitProvider.getRetrofit(context) }
    val musicApi = remember { retrofit.create(MusicApi::class.java) }

    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        runCatching { musicApi.getAlbums().albums }
            .onSuccess { albums = it.shuffled() }
            .onFailure { error = it.message }
        isLoading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Home") }) }) { padding ->
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
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.Start) {
            AsyncImage(model = album.songs.firstOrNull()?.thumbnail, contentDescription = album.name, modifier = Modifier.fillMaxWidth().height(120.dp))
            Text(album.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}