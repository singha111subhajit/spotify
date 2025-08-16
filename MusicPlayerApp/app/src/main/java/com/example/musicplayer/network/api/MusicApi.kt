package com.example.musicplayer.network.api

import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.Song
import retrofit2.http.GET

interface MusicApi {
    @GET("songs")
    suspend fun getSongs(): List<Song>

    @GET("playlists")
    suspend fun getPlaylists(): List<Playlist>
}