package com.example.DhoonHub.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.DhoonHub.model.Playlist
import com.example.DhoonHub.model.Song
import com.example.DhoonHub.network.RetrofitProvider
import com.example.DhoonHub.network.api.AddSongRequest
import com.example.DhoonHub.network.api.TrendingArtist
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class MusicRepository(private val context: Context) {
    private val musicApi = RetrofitProvider.getMusicApi(context)
    private val gson = Gson()

    companion object {
        private const val TAG = "MusicRepository"
    }

    suspend fun getSongsOnline(page: Int): List<Song> = musicApi.getSongs(page = page).songs
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

    fun getOfflineSongsWithMetadata(): List<Song> {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return emptyList()
        val metaFiles = dir.listFiles()?.filter {
            it.isFile && it.extension.lowercase() == "meta"
        } ?: emptyList()

        return metaFiles.mapNotNull { metaFile ->
            try {
                val json = metaFile.readText()
                gson.fromJson(json, Song::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read metadata file: ${metaFile.name}", e)
                null
            }
        }
    }

    private suspend fun downloadFile(url: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting download from $url to ${targetFile.absolutePath}")
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Download failed: Unsuccessful response ${response.code} for $url")
                    return@withContext false
                }
                val body = response.body
                if (body == null) {
                    Log.e(TAG, "Download failed: Response body is null for $url")
                    return@withContext false
                }
                FileOutputStream(targetFile).use { fos ->
                    body.byteStream().copyTo(fos)
                }
            }
            val fileExists = targetFile.exists()
            val fileLength = targetFile.length()
            Log.d(TAG, "Download finished for $url. File exists: $fileExists, File length: $fileLength")
            return@withContext fileExists && fileLength > 0
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $url", e)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            return@withContext false
        }
    }

    suspend fun downloadSong(song: Song): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "downloadSong started for ${song.title}")
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return@withContext false
        if (!dir.exists()) dir.mkdirs()

        val safeId = song.id ?: song.title
        val audioFileName = sanitizeFilename(safeId)
        val targetAudioFile = File(dir, "$audioFileName.mp3")
        val targetMetaFile = File(dir, "$audioFileName.meta")

        // 1. Download audio
        Log.d(TAG, "Downloading audio for ${song.title}")
        val audioDownloaded = if (targetAudioFile.exists()) {
            Log.d(TAG, "Audio for ${song.title} already exists.")
            true
        } else {
            downloadFile(song.url, targetAudioFile)
        }

        if (!audioDownloaded) {
            Log.e(TAG, "Audio download failed for ${song.title}")
            if(targetAudioFile.exists()) targetAudioFile.delete()
            return@withContext false
        }
        Log.d(TAG, "Audio download successful for ${song.title}")

        // 2. Download thumbnail
        var thumbnailDownloaded = true
        var targetThumbnailFile: File? = null
        song.thumbnail?.let { thumbnailUrl ->
            if (thumbnailUrl.isNotBlank()) {
                Log.d(TAG, "Downloading thumbnail for ${song.title}")
                val thumbnailsDir = File(dir, ".thumbnails")
                if (!thumbnailsDir.exists()) thumbnailsDir.mkdirs()
                val thumbnailFileName = sanitizeFilename(safeId)
                targetThumbnailFile = File(thumbnailsDir, "$thumbnailFileName.jpg")

                thumbnailDownloaded = if (targetThumbnailFile!!.exists()) {
                    Log.d(TAG, "Thumbnail for ${song.title} already exists.")
                    true
                } else {
                    downloadFile(thumbnailUrl, targetThumbnailFile!!)
                }
                
                if (!thumbnailDownloaded) {
                    Log.e(TAG, "Thumbnail download failed for ${song.title}")
                    targetThumbnailFile?.let {
                        if (it.exists()){
                            it.delete()
                        }
                    }
                } else {
                    Log.d(TAG, "Thumbnail download successful for ${song.title}")
                }
            }
        }

        if (!thumbnailDownloaded) {
            Log.e(TAG, "Thumbnail download failed, deleting audio file for ${song.title} to maintain consistency.")
            if (targetAudioFile.exists()) {
                targetAudioFile.delete()
            }
            return@withContext false
        }

        // 3. Create metadata file
        try {
            val offlineSong = song.copy(
                url = targetAudioFile.absolutePath,
                thumbnail = targetThumbnailFile?.absolutePath
            )
            val json = gson.toJson(offlineSong)
            targetMetaFile.writeText(json)
            Log.d(TAG, "Metadata file created for ${song.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create metadata file for ${song.title}", e)
            if (targetAudioFile.exists()) targetAudioFile.delete()
            targetThumbnailFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
            if (targetMetaFile.exists()) targetMetaFile.delete()
            return@withContext false
        }

        Log.d(TAG, "downloadSong finished successfully for ${song.title}")
        return@withContext true
    }

    fun getOfflineSongUri(file: File): Uri = Uri.fromFile(file)

    fun isSongDownloaded(song: Song): Boolean {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return false
        val safeId = song.id ?: song.title
        val targetMetaFile = File(dir, "${sanitizeFilename(safeId)}.meta")
        return targetMetaFile.exists()
    }

    fun deleteDownloadedSong(song: Song): Boolean {
        return deleteOfflineSong(song)
    }

    fun deleteOfflineSong(song: Song): Boolean {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return false
        val safeId = song.id ?: song.title
        val audioFileName = sanitizeFilename(safeId)
        val targetAudioFile = File(dir, "$audioFileName.mp3")
        val targetMetaFile = File(dir, "$audioFileName.meta")
        val thumbnailsDir = File(dir, ".thumbnails")
        val thumbnailFileName = sanitizeFilename(safeId)
        val targetThumbnailFile = File(thumbnailsDir, "$thumbnailFileName.jpg")

        var deleted = true
        if (targetAudioFile.exists()) {
            if (!targetAudioFile.delete()) deleted = false
        }
        if (targetMetaFile.exists()) {
            if (!targetMetaFile.delete()) deleted = false
        }
        if (targetThumbnailFile.exists()) {
            if (!targetThumbnailFile.delete()) deleted = false
        }
        return deleted
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
        val targetFile = File(dir, "${sanitizeFilename(songId)}.mp3")
        return targetFile
    }

    fun getOfflineThumbnailFile(songId: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return File("")
        val thumbnailsDir = File(dir, ".thumbnails")
        val targetFile = File(thumbnailsDir, "${sanitizeFilename(songId)}.jpg")
        return targetFile
    }

    suspend fun getPopularArtists(page: Int, limit: Int): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                musicApi.getPopularArtists(page, limit).artists
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getArtistDetails(artistName: String): TrendingArtist? {
        return withContext(Dispatchers.IO) {
            try {
                musicApi.getArtistDetails(artistName).artist
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getArtistSongs(artistName: String, page: Int, perPage: Int): List<Song> {
        return withContext(Dispatchers.IO) {
            try {
                musicApi.getArtistSongs(artistName, page, perPage).songs
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun searchArtistByName(name: String): TrendingArtist? {
        return withContext(Dispatchers.IO) {
            try {
                musicApi.getArtistDetails(name).artist
            } catch (e: Exception) {
                null
            }
        }
    }
}
