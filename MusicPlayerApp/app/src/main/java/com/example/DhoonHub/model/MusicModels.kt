package com.example.DhoonHub.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: String?,
    val title: String,
    val artist: String,
    val url: String,
    val thumbnail: String? = null,
    val album: String? = null
): Parcelable

data class Playlist(
    val id: Int,
    val name: String
)