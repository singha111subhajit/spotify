package com.example.DhoonHub.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String? = null,
    val artist: String? = null,
    val artworkUrl: String? = null,
    val currentUrl: String? = null,
    val isLocal: Boolean? = null
)

object PlaybackStateHolder {
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState = _uiState.asStateFlow()

    fun update(
        isPlaying: Boolean? = null,
        isShuffle: Boolean? = null,
        isRepeat: Boolean? = null,
        positionMs: Long? = null,
        durationMs: Long? = null,
        title: String? = null,
        artist: String? = null,
        artworkUrl: String? = null,
        currentUrl: String? = null,
        isLocal: Boolean? = null
    ) {
        _uiState.value = _uiState.value.copy(
            isPlaying = isPlaying ?: _uiState.value.isPlaying,
            isShuffle = isShuffle ?: _uiState.value.isShuffle,
            isRepeat = isRepeat ?: _uiState.value.isRepeat,
            positionMs = positionMs ?: _uiState.value.positionMs,
            durationMs = durationMs ?: _uiState.value.durationMs,
            title = title ?: _uiState.value.title,
            artist = artist ?: _uiState.value.artist,
            artworkUrl = artworkUrl ?: _uiState.value.artworkUrl,
            currentUrl = currentUrl ?: _uiState.value.currentUrl,
            isLocal = isLocal ?: _uiState.value.isLocal
        )
    }
}