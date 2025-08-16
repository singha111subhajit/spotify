package com.example.musicplayer.player

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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.example.musicplayer.MainActivity
import com.example.musicplayer.R
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class MusicPlayerService : Service() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            val audioAttr = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(audioAttr, true)
        }
        mediaSession = MediaSessionCompat(this, "MusicPlayerService").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { play() }
                override fun onPause() { pause() }
                override fun onSkipToNext() { next() }
                override fun onSkipToPrevious() { previous() }
                override fun onStop() { stopSelf() }
            })
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        when (intent?.action) {
            ACTION_PLAY_URL -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (!url.isNullOrBlank()) playUri(Uri.parse(url))
            }
            ACTION_PLAY_FILE -> {
                val path = intent.getStringExtra(EXTRA_FILE_PATH)
                if (!path.isNullOrBlank()) playUri(Uri.fromFile(java.io.File(path)))
            }
            ACTION_TOGGLE_PLAY_PAUSE -> if (player.isPlaying) pause() else play()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_TOGGLE_SHUFFLE -> {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                updateNotification()
            }
            ACTION_TOGGLE_REPEAT -> {
                player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                updateNotification()
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        player.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun setQueueAndPlay(uris: List<Uri>, startIndex: Int = 0) {
        player.setMediaItems(uris.map { MediaItem.fromUri(it) }, startIndex, 0L)
        player.prepare()
        player.playWhenReady = true
        updatePlaybackState()
        updateNotification()
    }

    private fun playUri(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
        updatePlaybackState()
        updateNotification()
    }

    private fun pause() { player.pause(); updatePlaybackState(); updateNotification() }
    private fun play() { player.play(); updatePlaybackState(); updateNotification() }
    private fun next() { player.seekToNextMediaItem(); updatePlaybackState(); updateNotification() }
    private fun previous() { player.seekToPreviousMediaItem(); updatePlaybackState(); updateNotification() }

    private fun updatePlaybackState() {
        val state = if (player.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
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
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
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
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
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

    companion object {
        private const val CHANNEL_ID = "music_playback"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_URL = "com.example.musicplayer.action.PLAY_URL"
        const val ACTION_PLAY_FILE = "com.example.musicplayer.action.PLAY_FILE"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.musicplayer.action.TOGGLE_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.musicplayer.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.musicplayer.action.PREVIOUS"
        const val ACTION_TOGGLE_SHUFFLE = "com.example.musicplayer.action.TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT = "com.example.musicplayer.action.TOGGLE_REPEAT"

        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILE_PATH = "extra_file_path"

        fun startPlayUrl(context: Context, url: String) {
            val intent = Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_PLAY_URL
                putExtra(EXTRA_URL, url)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun startPlayFile(context: Context, path: String) {
            val intent = Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_PLAY_FILE
                putExtra(EXTRA_FILE_PATH, path)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun sendControl(context: Context, action: String) {
            val intent = Intent(context, MusicPlayerService::class.java).apply { this.action = action }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}