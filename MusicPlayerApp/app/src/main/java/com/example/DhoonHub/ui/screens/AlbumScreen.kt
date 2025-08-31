package com.example.DhoonHub.ui.screens

import androidx.compose.ui.Modifier
import com.example.DhoonHub.player.DhoonHubService
import com.example.DhoonHub.viewmodel.MusicViewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Import ArrowBack
import androidx.compose.material.icons.filled.PlayArrow // Import PlayArrow
import com.example.DhoonHub.model.Song
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import com.example.DhoonHub.network.api.Album // Correct import statement
import androidx.compose.foundation.clickable // Import clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    rootNavController: NavController,
    albumName: String,
    musicViewModel: MusicViewModel
) {
    val context = LocalContext.current
    var songs by remember { mutableStateOf(listOf<Song>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(albumName) {
        loading = true
        // Fetch songs for the album
        songs = musicViewModel.getAlbumSongs(albumName)
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(albumName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(songs) { index, song ->
                    ListItem(
                        modifier = Modifier.clickable {
                            DhoonHubService.startPlayUrl(
                                context,
                                songs,
                                index
                            )
                            rootNavController.navigate("player")
                        },
                        leadingContent = {
                            AsyncImage(
                                model = song.thumbnail,
                                contentDescription = song.title
                            )
                        },
                        headlineContent = { Text(song.title) },
                        supportingContent = { Text(song.artist) },
                        trailingContent = {
                            IconButton(onClick = {
                                DhoonHubService.startPlayUrl(
                                    context,
                                    songs,
                                    index
                                )
                                rootNavController.navigate("player")
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play"
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}