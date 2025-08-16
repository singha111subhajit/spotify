package com.example.musicplayer.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val streamUrl: String
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList()
)