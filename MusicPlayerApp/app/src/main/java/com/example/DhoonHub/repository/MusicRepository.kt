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

    suspend fun getAlbumSongs(albumName: String): List<Song> = musicApi.getAlbumSongs(albumName).songs

    fun getOfflineSongs(): List<File> {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val files = dir?.listFiles()?.filter {
            it.isFile && it.extension.lowercase() in listOf("mp3", "m4a", "wav", "ogg", "flac")
        } ?: emptyList()
        return files.sortedBy { it.name }
    }

    /** ✅ New: get offline songs with thumbnails */
    fun getOfflineSongsWithMetadata(): List<Song> {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return emptyList()
        val files = dir.listFiles()?.filter {
            it.isFile && it.extension.lowercase() in listOf("mp3", "m4a", "wav", "ogg", "flac")
        } ?: emptyList()

        return files.map { file ->
            val nameParts = file.nameWithoutExtension.split("-")
            val artist = nameParts.getOrNull(0) ?: "Unknown Artist"
            val title = nameParts.getOrNull(1) ?: "Unknown Title"
            val id = nameParts.getOrNull(2) ?: file.nameWithoutExtension

            val thumbnailFile = getOfflineThumbnailFile(id)
            val thumbnailPath = if (thumbnailFile.exists()) thumbnailFile.absolutePath else null

            Song(
                id = id,
                title = title,
                artist = artist,
                url = file.absolutePath,
                thumbnail = thumbnailPath
            )
        }
    }

    private suspend fun downloadFile(url: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext false
            val body = response.body ?: return@withContext false
            FileOutputStream(targetFile).use { fos ->
                body.byteStream().copyTo(fos)
            }
        }
        true
    }

    suspend fun downloadSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return@withContext false
        if (!dir.exists()) dir.mkdirs()

        val safeId = song.id ?: song.title
        val audioFileName = sanitizeFilename("${song.artist}-${song.title}-${safeId}.mp3")
        val targetAudioFile = File(dir, audioFileName)

        // Download audio file
        if (!targetAudioFile.exists()) {
            val client = OkHttpClient()
            val request = Request.Builder().url(song.url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                FileOutputStream(targetAudioFile).use { fos ->
                    body.byteStream().copyTo(fos)
                }
            }
        }

        // Download thumbnail if available
        song.thumbnail?.let { thumbnailUrl ->
            if (thumbnailUrl.isNotBlank()) {
                val thumbnailsDir = File(dir, ".thumbnails")
                if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()
                val thumbnailFileName = sanitizeFilename("${safeId}.jpg") // Assuming JPG for thumbnails
                val targetThumbnailFile = File(thumbnailsDir, thumbnailFileName)
                if (!targetThumbnailFile.exists()) {
                    downloadFile(thumbnailUrl, targetThumbnailFile)
                }
            }
        }
        true
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

    suspend fun refreshDownloadState(songId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val file = getOfflineSongFile(songId)
            file.exists()
        }
    }

    private fun getOfflineSongFile(songId: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return File("")
        val targetFile = File(dir, sanitizeFilename("$songId.mp3"))
        return targetFile
    }

    fun getOfflineThumbnailFile(songId: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return File("")
        val thumbnailsDir = File(dir, ".thumbnails")
        val targetFile = File(thumbnailsDir, sanitizeFilename("$songId.jpg"))
        return targetFile
    }
}