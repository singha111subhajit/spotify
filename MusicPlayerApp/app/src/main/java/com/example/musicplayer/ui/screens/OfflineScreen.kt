package com.example.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.musicplayer.player.MusicPlayerService
import com.example.musicplayer.repository.MusicRepository
import java.io.File

@Composable
fun OfflineScreen(rootNav: NavController) {
    val context = LocalContext.current
    val repo = remember { MusicRepository(context) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(Unit) { files = repo.getOfflineSongs() }

    Scaffold(topBar = { TopAppBar(title = { Text("Offline") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(files) { file ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                MusicPlayerService.startPlayFile(context, file.absolutePath)
                                rootNav.navigate("player")
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(file.name, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = {
                            MusicPlayerService.startPlayFile(context, file.absolutePath)
                            rootNav.navigate("player")
                        }) { Text("Play") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}