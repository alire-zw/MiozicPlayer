package com.rero.miozicplayer.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rero.miozicplayer.bluetooth.BluetoothAvrcpManager
import com.rero.miozicplayer.data.MusicRepository
import com.rero.miozicplayer.data.NavDestination
import com.rero.miozicplayer.data.Playlist
import com.rero.miozicplayer.data.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(private val context: Context) : ViewModel() {

    private val repository = MusicRepository(context)
    private val bluetoothManager = BluetoothAvrcpManager(context)
    private var musicService: MusicPlayerService? = null
    private var positionJob: Job? = null
    private var bluetoothJob: Job? = null
    private var demoJob: Job? = null
    private var bound = false

    private fun isDemoMode(songs: List<Song> = _uiState.value.songs): Boolean {
        val song = songs.firstOrNull() ?: return true
        return song.uri == android.net.Uri.EMPTY || song.uri.toString().isBlank()
    }

    private val _uiState = MutableStateFlow(PlayerState())
    val uiState: StateFlow<PlayerState> = _uiState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as MusicPlayerService.LocalBinder).getService()
            musicService = service
            bound = true

            viewModelScope.launch {
                service.playerState.collect { serviceState ->
                if (_uiState.value.playbackMode == PlaybackMode.BLUETOOTH &&
                    _uiState.value.currentNav == NavDestination.BLUETOOTH &&
                    isBluetoothActive()
                ) {
                        return@collect
                    }
                    val trackChanged = serviceState.currentIndex != _uiState.value.currentIndex
                    _uiState.value = _uiState.value.copy(
                        songs = serviceState.songs,
                        currentIndex = serviceState.currentIndex,
                        isPlaying = serviceState.isPlaying,
                        positionMs = if (trackChanged) 0L else serviceState.positionMs.takeIf { it > 0 }
                            ?: _uiState.value.positionMs,
                        durationMs = serviceState.durationMs.takeIf { it > 0 }
                            ?: _uiState.value.durationMs,
                        isShuffleOn = serviceState.isShuffleOn,
                        isLoading = serviceState.isLoading,
                    )
                }
            }
            startPositionUpdates()
            syncSongsWithService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            bound = false
        }
    }

    init {
        bindService()
        startBluetoothMonitoring()
        loadBrowseFolders(markAppReady = true)
    }

    fun startBluetoothMonitoring() {
        bluetoothManager.start()
        bluetoothJob?.cancel()
        bluetoothJob = viewModelScope.launch {
            bluetoothManager.playbackInfo.collect { info ->
                applyBluetoothInfo(info)
            }
        }
    }

    fun stopBluetoothMonitoring() {
        bluetoothJob?.cancel()
        bluetoothManager.stop()
    }

    private fun applyBluetoothInfo(info: com.rero.miozicplayer.bluetooth.BluetoothPlaybackInfo) {
        val localPlaybackActive = _uiState.value.playbackMode == PlaybackMode.LOCAL &&
            _uiState.value.currentNav == NavDestination.BROWSE

        if (localPlaybackActive) {
            _uiState.value = _uiState.value.copy(
                isBluetoothConnected = info.isConnected,
                bluetoothDeviceName = info.deviceName,
            )
            return
        }

        val hasRemoteTrack = info.title.isNotBlank() || info.artist.isNotBlank()
        val remoteActive = info.isConnected && (info.isPlaying || hasRemoteTrack)

        if (remoteActive && _uiState.value.playbackMode != PlaybackMode.BLUETOOTH) {
            musicService?.pause()
        }

        _uiState.value = _uiState.value.copy(
            isBluetoothConnected = info.isConnected,
            bluetoothDeviceName = info.deviceName,
            bluetoothTitle = if (info.isConnected) info.title else "",
            bluetoothArtist = if (info.isConnected) info.artist else "",
            bluetoothAlbumArtUri = if (info.isConnected) info.albumArtUri else null,
            playbackMode = when {
                remoteActive -> PlaybackMode.BLUETOOTH
                !info.isConnected && _uiState.value.currentNav == NavDestination.BLUETOOTH ->
                    PlaybackMode.BLUETOOTH
                else -> _uiState.value.playbackMode
            },
            isPlaying = if (remoteActive) info.isPlaying else _uiState.value.isPlaying,
            positionMs = if (remoteActive) info.positionMs else _uiState.value.positionMs,
            durationMs = if (remoteActive && info.durationMs > 0) {
                info.durationMs
            } else {
                _uiState.value.durationMs
            },
        )
    }

    private fun isLocalPlaybackActive(): Boolean =
        _uiState.value.playbackMode == PlaybackMode.LOCAL &&
            _uiState.value.currentNav == NavDestination.BROWSE

    private fun enterLocalPlayback() {
        if (_uiState.value.playbackMode == PlaybackMode.LOCAL) return
        _uiState.value = _uiState.value.copy(playbackMode = PlaybackMode.LOCAL)
    }

    private fun isBluetoothActive(): Boolean =
        _uiState.value.playbackMode == PlaybackMode.BLUETOOTH && _uiState.value.isBluetoothConnected

    fun refreshBrowseFolders() {
        loadBrowseFolders()
    }

    private fun loadBrowseFolders(markAppReady: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val startMs = System.currentTimeMillis()
            val folders = repository.loadBrowseFolders()
            if (markAppReady) {
                val minSplashMs = 1200L
                val elapsed = System.currentTimeMillis() - startMs
                if (elapsed < minSplashMs) {
                    delay(minSplashMs - elapsed)
                }
            }
            _uiState.value = _uiState.value.copy(
                browseFolders = folders,
                isLoading = false,
                isAppReady = if (markAppReady) true else _uiState.value.isAppReady,
            )
        }
    }

    fun openBrowseFolder(folderId: String) {
        val folder = _uiState.value.browseFolders.find { it.id == folderId } ?: return
        _uiState.value = _uiState.value.copy(
            browseOpenedFolderId = folderId,
            playlist = Playlist(
                id = folder.id,
                title = folder.title,
                description = folder.description,
                songs = folder.songs,
            ),
        )
    }

    fun closeBrowseFolder() {
        _uiState.value = _uiState.value.copy(
            browseOpenedFolderId = null,
            playlist = null,
        )
    }

    private fun syncSongsWithService() {
        val songs = _uiState.value.songs
        if (songs.isEmpty()) return
        musicService?.setSongs(songs, _uiState.value.currentIndex)
        if (_uiState.value.isPlaying && isLocalPlaybackActive()) {
            musicService?.play()
        }
    }

    private fun bindService() {
        val intent = Intent(context, MusicPlayerService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.playbackMode == PlaybackMode.BLUETOOTH) {
                    val state = _uiState.value
                    if (state.isPlaying && state.durationMs > 0) {
                        val newPos = (state.positionMs + 250).coerceAtMost(state.durationMs)
                        _uiState.value = state.copy(positionMs = newPos)
                    }
                } else {
                    musicService?.let { service ->
                        val pos = service.getCurrentPosition()
                        val current = _uiState.value.positionMs
                        val isLoading = service.playerState.value.isLoading
                        if (!isLoading || pos > 0 || current == 0L) {
                            _uiState.value = _uiState.value.copy(positionMs = pos)
                        }
                    }
                }
                delay(250)
            }
        }
    }

    fun togglePlayPause() {
        if (isBluetoothActive()) {
            bluetoothManager.togglePlayPause()
            _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
            return
        }
        if (isDemoMode()) {
            val playing = !_uiState.value.isPlaying
            _uiState.value = _uiState.value.copy(isPlaying = playing)
            if (playing) startDemoPlayback() else demoJob?.cancel()
            return
        }
        val service = musicService
        if (service != null) {
            service.togglePlayPause()
        } else {
            _uiState.value = _uiState.value.copy(isPlaying = !_uiState.value.isPlaying)
        }
    }

    private fun startDemoPlayback() {
        demoJob?.cancel()
        demoJob = viewModelScope.launch {
            while (isActive && _uiState.value.isPlaying) {
                delay(500)
                val state = _uiState.value
                val newPos = state.positionMs + 500
                if (newPos >= state.durationMs) {
                    skipToNext()
                } else {
                    _uiState.value = state.copy(positionMs = newPos)
                }
            }
        }
    }

    fun skipToNext() {
        if (isBluetoothActive()) {
            bluetoothManager.skipToNext()
            return
        }
        if (isDemoMode()) {
            val next = (_uiState.value.currentIndex + 1) % _uiState.value.songs.size.coerceAtLeast(1)
            _uiState.value = _uiState.value.copy(
                currentIndex = next,
                positionMs = 0L,
                durationMs = _uiState.value.songs.getOrNull(next)?.durationMs ?: 0L,
            )
            return
        }
        musicService?.skipToNext() ?: run {
            val next = (_uiState.value.currentIndex + 1) % _uiState.value.songs.size.coerceAtLeast(1)
            _uiState.value = _uiState.value.copy(
                currentIndex = next,
                positionMs = 0L,
                durationMs = _uiState.value.songs.getOrNull(next)?.durationMs ?: 0L,
            )
        }
    }

    fun skipToPrevious() {
        if (isBluetoothActive()) {
            bluetoothManager.skipToPrevious()
            return
        }
        if (isDemoMode()) {
            val prev = if (_uiState.value.currentIndex > 0) _uiState.value.currentIndex - 1
            else _uiState.value.songs.lastIndex
            _uiState.value = _uiState.value.copy(
                currentIndex = prev,
                positionMs = 0L,
                durationMs = _uiState.value.songs.getOrNull(prev)?.durationMs ?: 0L,
            )
            return
        }
        musicService?.skipToPrevious() ?: run {
            val prev = if (_uiState.value.currentIndex > 0) _uiState.value.currentIndex - 1
            else _uiState.value.songs.lastIndex
            _uiState.value = _uiState.value.copy(
                currentIndex = prev,
                positionMs = 0L,
                durationMs = _uiState.value.songs.getOrNull(prev)?.durationMs ?: 0L,
            )
        }
    }

    fun seekForward15() {
        if (isBluetoothActive()) {
            bluetoothManager.seekForward15()
            return
        }
        val newPos = (_uiState.value.positionMs + 15_000).coerceAtMost(_uiState.value.durationMs)
        if (isDemoMode()) {
            _uiState.value = _uiState.value.copy(positionMs = newPos)
            return
        }
        _uiState.value = _uiState.value.copy(positionMs = newPos)
        musicService?.seekForward15()
    }

    fun seekBackward15() {
        if (isBluetoothActive()) {
            bluetoothManager.seekBackward15()
            return
        }
        val newPos = (_uiState.value.positionMs - 15_000).coerceAtLeast(0)
        if (isDemoMode()) {
            _uiState.value = _uiState.value.copy(positionMs = newPos)
            return
        }
        _uiState.value = _uiState.value.copy(positionMs = newPos)
        musicService?.seekBackward15()
    }

    fun seekTo(fraction: Float) {
        if (isBluetoothActive()) return
        if (isDemoMode()) {
            val target = (fraction * _uiState.value.durationMs).toLong()
            _uiState.value = _uiState.value.copy(positionMs = target)
            return
        }
        val target = (fraction * _uiState.value.durationMs).toLong()
        musicService?.seekTo(target)
        _uiState.value = _uiState.value.copy(positionMs = target)
    }

    fun toggleShuffle() {
        if (isBluetoothActive()) return
        musicService?.toggleShuffle()
        _uiState.value = _uiState.value.copy(isShuffleOn = !_uiState.value.isShuffleOn)
    }

    fun playSongAt(index: Int) {
        enterLocalPlayback()
        val targetSongs = playlistSongsForNav(_uiState.value.currentNav)
        if (targetSongs.isEmpty()) return
        val safeIndex = index.coerceIn(0, targetSongs.lastIndex)

        if (targetSongs != _uiState.value.songs) {
            switchToPlaylist(targetSongs, safeIndex)
            return
        }

        if (isDemoMode(targetSongs)) {
            _uiState.value = _uiState.value.copy(
                currentIndex = safeIndex,
                positionMs = 0L,
                durationMs = targetSongs.getOrNull(safeIndex)?.durationMs ?: 0L,
                isPlaying = true,
            )
            startDemoPlayback()
            return
        }
        musicService?.playSongAt(safeIndex) ?: run {
            _uiState.value = _uiState.value.copy(
                currentIndex = safeIndex,
                positionMs = 0L,
                durationMs = targetSongs.getOrNull(safeIndex)?.durationMs ?: 0L,
                isPlaying = true,
            )
        }
    }

    private fun playlistSongsForNav(nav: NavDestination): List<Song> = when (nav) {
        NavDestination.BLUETOOTH -> emptyList()
        NavDestination.BROWSE -> _uiState.value.browseDisplaySongs
    }

    private fun switchToPlaylist(songs: List<Song>, index: Int) {
        enterLocalPlayback()
        if (isDemoMode(songs)) {
            _uiState.value = _uiState.value.copy(
                songs = songs,
                currentIndex = index,
                positionMs = 0L,
                durationMs = songs.getOrNull(index)?.durationMs ?: 0L,
                isPlaying = true,
            )
            startDemoPlayback()
            return
        }
        val service = musicService
        if (service != null) {
            service.setSongs(songs, index)
            service.play()
        }
        _uiState.value = _uiState.value.copy(
            songs = songs,
            currentIndex = index,
            positionMs = 0L,
            durationMs = songs.getOrNull(index)?.durationMs ?: 0L,
            isPlaying = true,
            playbackMode = PlaybackMode.LOCAL,
        )
    }

    fun navigateTo(destination: NavDestination) {
        _uiState.value = _uiState.value.copy(
            currentNav = destination,
            browseOpenedFolderId = if (destination == NavDestination.BROWSE) {
                _uiState.value.browseOpenedFolderId
            } else {
                null
            },
        )
        when (destination) {
            NavDestination.BLUETOOTH -> {
                _uiState.value = _uiState.value.copy(playbackMode = PlaybackMode.BLUETOOTH)
                if (_uiState.value.isBluetoothConnected) {
                    musicService?.pause()
                }
            }
            NavDestination.BROWSE -> {
                if (_uiState.value.browseFolders.isEmpty()) {
                    loadBrowseFolders()
                }
            }
        }
    }

    override fun onCleared() {
        positionJob?.cancel()
        bluetoothJob?.cancel()
        demoJob?.cancel()
        stopBluetoothMonitoring()
        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
        }
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(context) as T
        }
    }
}
