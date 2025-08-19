package com.example.DhoonHub.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.DhoonHub.model.Playlist
import com.example.DhoonHub.model.Song
import com.example.DhoonHub.network.RetrofitProvider
import com.example.DhoonHub.network.api.AddSongRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class MusicRepository(private val context: Context) {
    private val musicApi = RetrofitProvider.getMusicApi(context)

    suspend fun getSongsOnline(): List<Song> = musicApi.getSongs().songs
    suspend fun searchSongsOnline(query: String, page: Int = 1, perPage: Int = 20): List<Song> =
        musicApi.search(q = query, page = page, perPage = perPage).songs
    suspend fun getPlaylistsOnline(): List<Playlist> = musicApi.getPlaylists().playlists
    suspend fun getPlaylistSongs(playlistId: Int) = musicApi.getPlaylistSongs(playlistId).songs
    suspend fun addSongToPlaylist(playlistId: Int, songId: String, songTitle: String) =
        musicApi.addSongToPlaylist(playlistId, AddSongRequest(song_id = songId, song_title = songTitle))
    suspend fun removeSongFromPlaylist(playlistId: Int, songDbId: Int) =
        musicApi.removeSongFromPlaylist(playlistId, songDbId)

    fun getOfflineSongs(): List<File> {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val files = dir?.listFiles()?.filter { 
            it.isFile && it.extension.lowercase() in listOf("mp3", "m4a", "wav", "ogg", "flac") 
        } ?: emptyList()
        return files.sortedBy { it.name }
    }

    suspend fun downloadSong(song: Song): File? = withContext(Dispatchers.IO) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return@withContext null
        if (!dir.exists()) dir.mkdirs()
        val safeId = song.id ?: song.title
        val targetFile = File(dir, sanitizeFilename("${song.artist}-${song.title}-${safeId}.mp3"))
        if (targetFile.exists()) return@withContext targetFile

        val client = OkHttpClient()
        val request = Request.Builder().url(song.url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body ?: return@withContext null
            FileOutputStream(targetFile).use { fos ->
                body.byteStream().copyTo(fos)
            }
        }
        targetFile
    }

    fun getOfflineSongUri(file: File): Uri = Uri.fromFile(file)

    fun isSongDownloaded(song: Song): Boolean {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return false
        val safeId = song.id ?: song.title
        val targetFile = File(dir, sanitizeFilename("${song.artist}-${song.title}-${safeId}.mp3"))
        return targetFile.exists()
    }

    fun deleteDownloadedSong(song: Song): Boolean {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return false
        val safeId = song.id ?: song.title
        val targetFile = File(dir, sanitizeFilename("${song.artist}-${song.title}-${safeId}.mp3"))
        return if (targetFile.exists()) {
            targetFile.delete()
        } else false
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}