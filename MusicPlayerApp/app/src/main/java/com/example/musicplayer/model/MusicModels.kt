package com.example.musicplayer.model

data class Song(
    val id: String?,
    val title: String,
    val artist: String,
    val url: String,
    val thumbnail: String? = null
)

data class Playlist(
    val id: Int,
    val name: String
)