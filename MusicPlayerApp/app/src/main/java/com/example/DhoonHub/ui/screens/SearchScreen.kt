
package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.DhoonHub.R
import com.example.DhoonHub.model.Song
import com.example.DhoonHub.viewmodel.MusicViewModel
import com.example.DhoonHub.player.DhoonHubService
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(rootNav: NavController, musicViewModel: MusicViewModel) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val artistSongs = musicViewModel.artistSongs
    val isLoadingArtistSongs = musicViewModel.isLoadingArtistSongs
    val artistSongsError = musicViewModel.artistSongsError

    fun triggerSearch() {
        if (query.length >= 2) {
            musicViewModel.loadArtistSongs(query)
        } else {
            musicViewModel.clearArtistSongs()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Search") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    triggerSearch()
                },
                label = { Text("Search for songs by artist…") },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            if (isLoadingArtistSongs) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (artistSongsError != null) {
                Text(
                    text = "Error: $artistSongsError",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(artistSongs) { song ->
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
                        trailingContent = {
                            TextButton(onClick = {
                                val url = song.url
                                if (url.isNotBlank()) {
                                    runCatching {
                                        DhoonHubService.startPlayUrl(context, 
                                            listOf(song),
                                            0)
                                        rootNav.navigate("player")
                                    }
                                }
                            }) { Text("Play") }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
