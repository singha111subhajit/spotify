package com.example.musicplayer.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.musicplayer.model.Playlist
import com.example.musicplayer.model.Song
import com.example.musicplayer.network.RetrofitProvider
import com.example.musicplayer.network.api.PlaylistsResponse
import com.example.musicplayer.network.api.SongsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class MusicRepository(private val context: Context) {
    private val musicApi = RetrofitProvider.getMusicApi(context)

    suspend fun getSongsOnline(): List<Song> = musicApi.getSongs().songs
    suspend fun getPlaylistsOnline(): List<Playlist> = musicApi.getPlaylists().playlists

    fun getOfflineSongs(): List<File> {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val files = dir?.listFiles()?.filter { it.isFile } ?: emptyList()
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

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}