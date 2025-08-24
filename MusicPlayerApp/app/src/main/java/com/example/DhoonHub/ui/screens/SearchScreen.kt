package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.DhoonHub.R
import com.example.DhoonHub.model.Song
import com.example.DhoonHub.network.RetrofitProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(rootNav: NavController) {
    val context = LocalContext.current
    val retrofit = remember { RetrofitProvider.getRetrofit(context) }
    val musicApi = remember { com.example.DhoonHub.network.api.MusicApi::class.java.let { retrofit.create(it) } }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun triggerSearch() {
        searchJob?.cancel()
        searchJob = scope.launch {
            loading = true
            delay(300)
            runCatching {
                val resp = musicApi.search(q = query)
                results = resp.songs
            }.onFailure { }
            loading = false
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Search") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (query.length >= 2) triggerSearch() else results = emptyList()
                },
                label = { Text("Search for songs, artists, albums, or playlists…") },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.fillMaxSize()) {
                items(results) { song ->
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
                                        com.example.DhoonHub.player.DhoonHubService.startPlayUrl(context, 
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