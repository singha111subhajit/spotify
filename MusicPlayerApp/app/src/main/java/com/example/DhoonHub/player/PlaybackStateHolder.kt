package com.example.DhoonHub.player

import android.content.Context
import com.example.DhoonHub.player.DhoonHubService.Companion.ACTION_SEEK_TO
import com.example.DhoonHub.player.DhoonHubService.Companion.EXTRA_POSITION_MS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val currentUrl: String? = null,
    val isLocal: Boolean = false
)

object PlaybackStateHolder {
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState

    fun update(
        isPlaying: Boolean? = null,
        positionMs: Long? = null,
        durationMs: Long? = null,
        title: String? = null,
        artist: String? = null,
        artworkUrl: String? = null,
        currentUrl: String? = null,
        isLocal: Boolean? = null
    ) {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            isPlaying = isPlaying ?: cur.isPlaying,
            positionMs = positionMs ?: cur.positionMs,
            durationMs = durationMs ?: cur.durationMs,
            title = title ?: cur.title,
            artist = artist ?: cur.artist,
            artworkUrl = artworkUrl ?: cur.artworkUrl,
            currentUrl = currentUrl ?: cur.currentUrl,
            isLocal = isLocal ?: cur.isLocal,
        )
    }

    fun seekTo(context: Context, positionMs: Long) {
        DhoonHubService.sendSeekTo(context, positionMs)
    }
}