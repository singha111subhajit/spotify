package com.example.DhoonHub.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.DhoonHub.player.DhoonHubService
import com.example.DhoonHub.ui.components.SongListItem
import com.example.DhoonHub.viewmodel.MusicViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.example.DhoonHub.player.PlaybackUiState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.fillMaxWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artistName: String,
    rootNavController: NavController,
    musicViewModel: MusicViewModel,
    playbackState: PlaybackUiState
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(artistName) {
        musicViewModel.loadArtistSongs(artistName)
    }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
        if (lastVisibleItem != null && lastVisibleItem.index >= musicViewModel.artistSongs.size - 5) {
            musicViewModel.loadMoreArtistSongs(artistName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(artistName) })
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            if (musicViewModel.isLoadingArtistSongs && musicViewModel.artistSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (musicViewModel.artistSongsError != null) {
                Text(text = "Error: ${musicViewModel.artistSongsError}")
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(musicViewModel.artistSongs) { song ->
                        val isPlaying = playbackState.currentUrl == song.url
                        SongListItem(song = song, isPlaying = isPlaying, onClick = {
                            DhoonHubService.startPlayUrl(
                                context,
                                musicViewModel.artistSongs,
                                musicViewModel.artistSongs.indexOf(song)
                            )
                            rootNavController.navigate("player")
                        })
                    }
                    if (musicViewModel.isPaginatingArtistSongs) {
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