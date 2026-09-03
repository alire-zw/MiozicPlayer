package com.rero.miozicplayer.player

import android.net.Uri
import com.rero.miozicplayer.data.BrowseFolder
import com.rero.miozicplayer.data.NavDestination
import com.rero.miozicplayer.data.Playlist
import com.rero.miozicplayer.data.Song

data class PlayerState(
    val playlist: Playlist? = null,
    val browseFolders: List<BrowseFolder> = emptyList(),
    val browseOpenedFolderId: String? = null,
    val songs: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleOn: Boolean = false,
    val isLoading: Boolean = false,
    val isAppReady: Boolean = false,
    val currentNav: NavDestination = NavDestination.BLUETOOTH,
    val playbackMode: PlaybackMode = PlaybackMode.BLUETOOTH,
    val isBluetoothConnected: Boolean = false,
    val bluetoothDeviceName: String? = null,
    val bluetoothTitle: String = "",
    val bluetoothArtist: String = "",
    val bluetoothAlbumArtUri: Uri? = null,
) {
    val isBluetoothMode: Boolean
        get() = playbackMode == PlaybackMode.BLUETOOTH

    val currentSong: Song?
        get() = songs.getOrNull(currentIndex)

    val openedBrowseFolder: BrowseFolder?
        get() = browseOpenedFolderId?.let { id ->
            browseFolders.find { it.id == id }
        }

    val browseDisplaySongs: List<Song>
        get() = openedBrowseFolder?.songs ?: emptyList()

    val displayTitle: String
        get() = if (isBluetoothMode && bluetoothTitle.isNotBlank()) {
            bluetoothTitle
        } else {
            currentSong?.title ?: if (isBluetoothMode) "Bluetooth Audio" else "No Track"
        }

    val displayArtist: String
        get() = if (isBluetoothMode) {
            bluetoothArtist
        } else {
            currentSong?.artist ?: ""
        }

    val displayAlbumArtUri: Uri?
        get() = if (isBluetoothMode) bluetoothAlbumArtUri else currentSong?.albumArtUri
}
