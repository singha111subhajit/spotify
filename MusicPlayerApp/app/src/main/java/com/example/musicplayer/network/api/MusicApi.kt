package com.example.musicplayer.network.api

import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.Song
import retrofit2.http.GET
import retrofit2.http.Query

data class SongsResponse(val songs: List<Song>)
data class PlaylistsResponse(val playlists: List<Playlist>)
data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val song_count: Int,
    val songs: List<Song> = emptyList(),
)
data class AlbumsResponse(val albums: List<Album>)

data class ArtistsResponse(val artists: List<ArtistMeta>)
data class ArtistMeta(val name: String, val song_count: Int, val album_count: Int, val albums: List<String>)

interface MusicApi {
    @GET("api/songs")
    suspend fun getSongs(): SongsResponse

    @GET("playlists")
    suspend fun getPlaylists(): PlaylistsResponse

    @GET("api/search")
    suspend fun search(@Query("q") q: String): SongsResponse

    @GET("api/albums")
    suspend fun getAlbums(): AlbumsResponse

    @GET("api/artists")
    suspend fun getArtists(): ArtistsResponse
}