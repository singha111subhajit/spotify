package com.example.musicplayer.player

import android.content.Context
import com.example.musicplayer.player.MusicPlayerService.Companion.ACTION_SEEK_TO
import com.example.musicplayer.player.MusicPlayerService.Companion.EXTRA_POSITION_MS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null
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
        artworkUrl: String? = null
    ) {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            isPlaying = isPlaying ?: cur.isPlaying,
            positionMs = positionMs ?: cur.positionMs,
            durationMs = durationMs ?: cur.durationMs,
            title = title ?: cur.title,
            artist = artist ?: cur.artist,
            artworkUrl = artworkUrl ?: cur.artworkUrl,
        )
    }

    fun seekTo(context: Context, positionMs: Long) {
        MusicPlayerService.sendSeekTo(context, positionMs)
    }
}