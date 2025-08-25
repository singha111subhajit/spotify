package com.example.DhoonHub.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.example.DhoonHub.MainActivity
import com.example.DhoonHub.R
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.DhoonHub.model.Song
import com.example.DhoonHub.repository.MusicRepository
import kotlinx.coroutines.*
import java.io.File

class DhoonHubService : Service() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private var tickerJob: Job? = null
    private var currentPlaylist: List<Song> = emptyList()
    private var currentPlaylistIndex: Int = -1
    private lateinit var repo: MusicRepository

    override fun onCreate() {
        super.onCreate()
        repo = MusicRepository(applicationContext)

        // Build ExoPlayer with proper audio attributes
        player = ExoPlayer.Builder(this).build().apply {
            val audioAttr = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttr, true)
        }

        // Setup MediaSession for integration with Android system
        mediaSession = MediaSessionCompat(this, "DhoonHubService").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = play()
                override fun onPause() = pause()
                override fun onSkipToNext() = next()
                override fun onSkipToPrevious() = previous()
                override fun onStop() = stopSelf()
            })
        }

        // Player listeners
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                PlaybackStateHolder.update(isPlaying = isPlaying)
                if (isPlaying) startTicker() else stopTicker()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.errorCodeName}", error)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = runCatching { player.duration }.getOrElse { 0L }
                    if (dur > 0) PlaybackStateHolder.update(durationMs = dur)
                } else if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Playback state changed to STATE_ENDED. Repeat mode: ${player.repeatMode}")
                }
            }

            // 🔑 This is the important fix
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d(TAG, "Media item transition. Reason: $reason. Repeat mode: ${player.repeatMode}")
                if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                    // If repeat one is enabled, don't change the index, just loop the current song
                    return
                }
                currentPlaylistIndex = player.currentMediaItemIndex
                val song = currentPlaylist.getOrNull(currentPlaylistIndex)
                if (song != null) {
                    updateCurrentSongInfo(song)
                }
                updatePlaybackState()
                updateNotification()
            }
        })

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return try {
            MediaButtonReceiver.handleIntent(mediaSession, intent)
            when (intent?.action) {
                ACTION_PLAY_URL -> {
                    val playlist = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(EXTRA_PLAYLIST, Song::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra(EXTRA_PLAYLIST)
                    }
                    val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
                    if (!playlist.isNullOrEmpty()) {
                        setPlaylistAndPlay(playlist as ArrayList<Song>, startIndex)
                    }
                }
                ACTION_PLAY_FILE -> {
                    val song = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(EXTRA_SONG, Song::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(EXTRA_SONG)
                    }
                    Log.d(TAG, "Received song: $song")
                    if (song != null) {
                        val offlineSongs = repo.getOfflineSongsWithMetadata()
                        val startIndex = offlineSongs.indexOf(song)
                        setPlaylistAndPlay(offlineSongs, if (startIndex != -1) startIndex else 0)
                    }
                }
                ACTION_TOGGLE_PLAY_PAUSE -> if (player.isPlaying) pause() else play()
                ACTION_NEXT -> next()
                ACTION_PREVIOUS -> previous()
                ACTION_TOGGLE_SHUFFLE -> {
                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                    PlaybackStateHolder.update(isShuffle = player.shuffleModeEnabled)
                    updateNotification()
                }
                ACTION_TOGGLE_REPEAT -> {
                    player.repeatMode =
                        if (player.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE
                        else Player.REPEAT_MODE_OFF
                    Log.d(TAG, "Repeat mode set to: ${player.repeatMode}")
                    PlaybackStateHolder.update(isRepeat = player.repeatMode == Player.REPEAT_MODE_ONE)
                    updateNotification()
                }
                ACTION_SEEK_TO -> {
                    val pos = intent.getLongExtra(EXTRA_POSITION_MS, -1L)
                    if (pos >= 0) {
                        player.seekTo(pos)
                        PlaybackStateHolder.update(positionMs = pos)
                    }
                }
            }
            START_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand error", e)
            START_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        player.release()
        stopTicker()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun setPlaylistAndPlay(songs: List<Song>, startIndex: Int = 0) {
        currentPlaylist = songs
        currentPlaylistIndex = startIndex

        val mediaItems = songs.map { 
            if (it.url.startsWith("http")) {
                MediaItem.fromUri(Uri.parse(it.url))
            } else {
                MediaItem.fromUri(Uri.fromFile(File(it.url)))
            }
        }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.playWhenReady = true

        val currentSong = songs.getOrNull(startIndex)
        if (currentSong != null) {
            updateCurrentSongInfo(currentSong)
        }
        updatePlaybackState()
        updateNotification()
    }

    private fun updateMetadata(title: String?, artist: String?, artworkUrl: String?) {
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title ?: "")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist ?: "")
        mediaSession.setMetadata(builder.build())
    }

    private fun pause() {
        player.pause()
        updatePlaybackState()
        updateNotification()
        PlaybackStateHolder.update(isPlaying = false)
    }

    private fun play() {
        player.play()
        updatePlaybackState()
        updateNotification()
        PlaybackStateHolder.update(isPlaying = true)
    }

    private fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (currentPlaylist.isNotEmpty()) {
            player.seekToDefaultPosition(0) // loop back
        }
    }

    private fun previous() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else if (currentPlaylist.isNotEmpty()) {
            player.seekToDefaultPosition(currentPlaylist.size - 1) // loop back
        }
    }

    private fun updateCurrentSongInfo(song: Song) {
        updateMetadata(song.title, song.artist, song.thumbnail)
        val dur = runCatching { player.duration }.getOrElse { 0L }
        val isLocal = song.url.startsWith("file")
        PlaybackStateHolder.update(
            title = song.title,
            artist = song.artist,
            artworkUrl = song.thumbnail,
            durationMs = if (dur > 0) dur else PlaybackStateHolder.uiState.value.durationMs,
            currentUrl = song.url,
            isLocal = isLocal
        )
    }

    private fun updatePlaybackState() {
        val state =
            if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(state, player.currentPosition, 1.0f)
                .build()
        )
        val dur = runCatching { player.duration }.getOrElse { PlaybackStateHolder.uiState.value.durationMs }
        val currentSong = currentPlaylist.getOrNull(currentPlaylistIndex)

        PlaybackStateHolder.update(
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition,
            durationMs = if (dur > 0) dur else PlaybackStateHolder.uiState.value.durationMs,
            title = currentSong?.title,
            artist = currentSong?.artist,
            artworkUrl = currentSong?.thumbnail,
            currentUrl = currentSong?.url,
            isLocal = currentSong?.url?.startsWith("file")
        )
    }

    private fun buildNotification(): Notification {
        val activityIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Music playback")
            .setContentIntent(pi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken))
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_launcher_foreground,
                    getString(R.string.previous),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_launcher_foreground,
                    if (player.isPlaying) getString(R.string.pause) else getString(R.string.play),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        if (player.isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_launcher_foreground,
                    getString(R.string.next),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
        return builder.build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.notification_channel_desc)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = CoroutineScope(Dispatchers.Main.immediate).launch {
            while (isActive) {
                try {
                    val pos = runCatching { player.currentPosition }.getOrElse { 0L }
                    val dur = runCatching { player.duration }
                        .getOrElse { PlaybackStateHolder.uiState.value.durationMs }
                    val safeDur = if (dur > 0) dur else PlaybackStateHolder.uiState.value.durationMs
                    PlaybackStateHolder.update(positionMs = pos.coerceAtLeast(0L), durationMs = safeDur)
                } catch (e: Exception) {
                    Log.e(TAG, "ticker error", e)
                    break
                }
                delay(250)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    companion object {
        private const val TAG = "DhoonHubService"
        private const val CHANNEL_ID = "music_playback"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_URL = "com.example.DhoonHub.action.PLAY_URL"
        const val ACTION_PLAY_FILE = "com.example.DhoonHub.action.PLAY_FILE"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.DhoonHub.action.TOGGLE_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.DhoonHub.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.DhoonHub.action.PREVIOUS"
        const val ACTION_TOGGLE_SHUFFLE = "com.example.DhoonHub.action.TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT = "com.example.DhoonHub.action.TOGGLE_REPEAT"
        const val ACTION_SEEK_TO = "com.example.DhoonHub.action.SEEK_TO"

        const val EXTRA_URL = "extra_url"
        const val EXTRA_SONG = "extra_song"
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_ARTWORK = "extra_artwork"
        const val EXTRA_POSITION_MS = "extra_position_ms"

        const val EXTRA_PLAYLIST = "extra_playlist"
        const val EXTRA_START_INDEX = "extra_start_index"

        fun startPlayUrl(context: Context, songs: List<Song>, startIndex: Int = 0) {
            val intent = Intent(context, DhoonHubService::class.java).apply {
                action = ACTION_PLAY_URL
                putParcelableArrayListExtra(EXTRA_PLAYLIST, ArrayList(songs))
                putExtra(EXTRA_START_INDEX, startIndex)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun startPlayFile(context: Context, song: Song) {
            val intent = Intent(context, DhoonHubService::class.java).apply {
                action = ACTION_PLAY_FILE
                putExtra(EXTRA_SONG, song)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun sendControl(context: Context, action: String) {
            val intent = Intent(context, DhoonHubService::class.java).apply { this.action = action }
            ContextCompat.startForegroundService(context, intent)
        }

        fun sendSeekTo(context: Context, positionMs: Long) {
            val intent = Intent(context, DhoonHubService::class.java).apply {
                action = ACTION_SEEK_TO
                putExtra(EXTRA_POSITION_MS, positionMs)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}