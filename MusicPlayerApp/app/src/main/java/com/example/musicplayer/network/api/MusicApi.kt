package com.example.musicplayer.network.api

import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.Song
import retrofit2.http.GET
import retrofit2.http.Query

data class SongsResponse(val songs: List<Song>)
data class PlaylistsResponse(val playlists: List<Playlist>)

interface MusicApi {
    @GET("api/songs")
    suspend fun getSongs(): SongsResponse

    @GET("playlists")
    suspend fun getPlaylists(): PlaylistsResponse

    @GET("api/search")
    suspend fun search(@Query("q") q: String): SongsResponse
}