package com.example.DhoonHub.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.DhoonHub.R
import com.example.DhoonHub.network.api.TrendingArtist
import com.example.DhoonHub.viewmodel.MusicViewModel

@Composable
fun ArtistCard(artistName: String, musicViewModel: MusicViewModel, onClick: () -> Unit) {
    var artist by remember { mutableStateOf<TrendingArtist?>(null) }

    LaunchedEffect(artistName) {
        artist = musicViewModel.getArtistDetails(artistName)
    }

    LaunchedEffect(artist) {
        android.util.Log.d("DhoonHub", "ArtistCard - Artist: $artist")
    }

    Card(
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = artist?.image,
                contentDescription = artist?.name,
                modifier = Modifier
                    .size(120.dp)
                    .clip(MaterialTheme.shapes.medium),
                placeholder = painterResource(R.drawable.ic_music_note),
                error = painterResource(R.drawable.ic_music_note),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = artistName,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
