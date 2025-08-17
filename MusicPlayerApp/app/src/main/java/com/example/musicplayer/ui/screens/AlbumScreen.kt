package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.musicplayer.network.RetrofitProvider
import com.example.musicplayer.network.api.MusicApi
import com.example.musicplayer.player.MusicPlayerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(rootNav: NavController, albumName: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val retrofit = remember { RetrofitProvider.getRetrofit(context) }
    val musicApi = remember { retrofit.create(MusicApi::class.java) }
    var songs by remember { mutableStateOf(listOf<com.example.musicplayer.model.Song>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(albumName) {
        loading = true
        val albums = runCatching { musicApi.getAlbums().albums }.getOrDefault(emptyList())
        val album = albums.find { it.name == albumName }
        songs = album?.songs ?: emptyList()
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text(albumName) }, navigationIcon = {
        IconButton(onClick = { rootNav.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
    }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.fillMaxSize()) {
                items(songs) { song ->
                    ListItem(
                        leadingContent = { AsyncImage(model = song.thumbnail, contentDescription = song.title) },
                        headlineContent = { Text(song.title) },
                        supportingContent = { Text(song.artist) },
                        trailingContent = {
                            TextButton(onClick = {
                                MusicPlayerService.startPlayUrl(context, song.url, title = song.title, artist = song.artist, artworkUrl = song.thumbnail)
                                rootNav.navigate("player")
                            }) { Text("Play") }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}