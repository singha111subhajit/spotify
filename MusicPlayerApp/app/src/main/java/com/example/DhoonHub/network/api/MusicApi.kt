package com.example.DhoonHub.network.api

import com.example.DhoonHub.model.Playlist
import com.example.DhoonHub.model.Song
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

data class PlaylistSong(val id: Int, val song_id: String, val song_title: String)
data class PlaylistSongsResponse(val songs: List<PlaylistSong>)
data class AddSongRequest(val song_id: String, val song_title: String)

interface MusicApi {
    @GET("api/songs")
    suspend fun getSongs(): SongsResponse

    @GET("playlists")
    suspend fun getPlaylists(): PlaylistsResponse

    @GET("playlists/{playlist_id}/songs")
    suspend fun getPlaylistSongs(@Path("playlist_id") playlistId: Int): PlaylistSongsResponse

    @POST("playlists/{playlist_id}/songs")
    suspend fun addSongToPlaylist(@Path("playlist_id") playlistId: Int, @Body body: AddSongRequest): Map<String, Any>

    @DELETE("playlists/{playlist_id}/songs/{song_db_id}")
    suspend fun removeSongFromPlaylist(
        @Path("playlist_id") playlistId: Int,
        @Path("song_db_id") songDbId: Int
    ): Map<String, Any>

    @GET("api/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): SongsResponse

    @GET("api/albums")
    suspend fun getAlbums(@Query("page") page: Int? = null, @Query("per_page") perPage: Int? = null): AlbumsResponse

    @GET("api/artists")
    suspend fun getArtists(): ArtistsResponse

    @GET("api/album_songs")
    suspend fun getAlbumSongs(@Query("album") album: String): SongsResponse
}