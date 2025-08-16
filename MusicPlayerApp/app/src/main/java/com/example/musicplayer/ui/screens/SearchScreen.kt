package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.musicplayer.model.Song
import com.example.musicplayer.network.RetrofitProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(nav: NavController) {
    val context = LocalContext.current
    val retrofit = remember { RetrofitProvider.getRetrofit(context) }
    val musicApi = remember { com.example.musicplayer.network.api.MusicApi::class.java.let { retrofit.create(it) } }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("Search") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search songs or artists") },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                scope.launch {
                    loading = true
                    runCatching {
                        val resp = musicApi.search(q = query)
                        results = resp.songs
                    }.onFailure { }
                    loading = false
                }
            }, enabled = query.isNotBlank() && !loading) { Text("Search") }
            Spacer(Modifier.height(12.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.fillMaxSize()) {
                items(results) { song ->
                    ListItem(
                        headlineContent = { Text(song.title) },
                        supportingContent = { Text(song.artist) },
                        trailingContent = {
                            TextButton(onClick = {
                                com.example.musicplayer.player.MusicPlayerService.startPlayUrl(context, song.url)
                                nav.navigate("player")
                            }) { Text("Play") }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}