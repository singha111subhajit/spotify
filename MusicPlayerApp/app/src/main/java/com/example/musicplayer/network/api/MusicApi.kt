package com.example.musicplayer.network.api

import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.Song
import retrofit2.http.GET

data class SongsResponse(val songs: List<Song>)
data class PlaylistsResponse(val playlists: List<Playlist>)

interface MusicApi {
    @GET("api/songs")
    suspend fun getSongs(): SongsResponse

    @GET("playlists")
    suspend fun getPlaylists(): PlaylistsResponse
}