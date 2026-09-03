package com.rero.miozicplayer.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.rero.miozicplayer.MainActivity
import com.rero.miozicplayer.R
import com.rero.miozicplayer.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayerService : android.app.Service() {

    private val binder = LocalBinder()
    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState { copy(isPlaying = isPlaying) }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_TIMELINE_CHANGED,
                )
            ) {
                val position = exoPlayer?.currentPosition ?: 0L
                if (position > 0L) {
                    updateState { copy(positionMs = position) }
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    val duration = exoPlayer?.duration ?: 0L
                    val position = exoPlayer?.currentPosition ?: 0L
                    updateState {
                        copy(
                            durationMs = duration,
                            positionMs = position,
                            isLoading = false,
                        )
                    }
                }
                Player.STATE_BUFFERING -> updateState { copy(isLoading = true) }
                Player.STATE_ENDED -> skipToNext()
                Player.STATE_IDLE -> updateState { copy(isLoading = false, isPlaying = false) }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            updateState { copy(isLoading = false, isPlaying = false) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = exoPlayer?.currentMediaItemIndex ?: 0
            updateState { copy(currentIndex = index, positionMs = 0L) }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initPlayer()
    }

    private fun initPlayer() {
        val dataSourceFactory = DefaultDataSource.Factory(this)
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { player ->
                player.addListener(playerListener)
                mediaSession = MediaSession.Builder(this, player).build()
            }
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> skipToNext()
            ACTION_PREVIOUS -> skipToPrevious()
        }
        return START_STICKY
    }

    fun setSongs(songs: List<Song>, startIndex: Int = 0) {
        val player = exoPlayer ?: return
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .build(),
                )
                .build()
        }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        updateState {
            copy(
                songs = songs,
                currentIndex = startIndex,
                isLoading = true,
            )
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun skipToNext() {
        exoPlayer?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        exoPlayer?.let { player ->
            if (player.currentPosition > 3000) {
                player.seekTo(0)
            } else {
                player.seekToPreviousMediaItem()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        updateState { copy(positionMs = positionMs) }
    }

    fun seekForward15() {
        exoPlayer?.let { player ->
            val newPos = (player.currentPosition + 15_000)
                .coerceAtMost(player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
            player.seekTo(newPos)
            updateState { copy(positionMs = newPos) }
        }
    }

    fun seekBackward15() {
        exoPlayer?.let { player ->
            val newPos = (player.currentPosition - 15_000).coerceAtLeast(0)
            player.seekTo(newPos)
            updateState { copy(positionMs = newPos) }
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_playerState.value.isShuffleOn
        exoPlayer?.shuffleModeEnabled = newShuffle
        updateState { copy(isShuffleOn = newShuffle) }
    }

    fun playSongAt(index: Int) {
        exoPlayer?.seekTo(index, 0L)
        exoPlayer?.play()
        updateState { copy(currentIndex = index) }
    }

    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L

    fun updatePlayerState(transform: (PlayerState) -> PlayerState) {
        updateState(transform)
    }

    private fun updateState(transform: PlayerState.() -> PlayerState) {
        _playerState.value = _playerState.value.transform()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val song = _playerState.value.currentSong
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "Miozic Player")
            .setContentText(song?.artist ?: "No track playing")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        mediaSession?.release()
        exoPlayer?.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.rero.miozicplayer.PLAY_PAUSE"
        const val ACTION_NEXT = "com.rero.miozicplayer.NEXT"
        const val ACTION_PREVIOUS = "com.rero.miozicplayer.PREVIOUS"
        private const val CHANNEL_ID = "music_playback"
        private const val NOTIFICATION_ID = 1
    }
}
