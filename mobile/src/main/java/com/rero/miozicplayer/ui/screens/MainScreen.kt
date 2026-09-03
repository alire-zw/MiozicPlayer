package com.rero.miozicplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.rero.miozicplayer.data.NavDestination
import com.rero.miozicplayer.player.PlayerViewModel
import com.rero.miozicplayer.ui.components.NavSidebar
import com.rero.miozicplayer.ui.components.NowPlayingPanel
import com.rero.miozicplayer.ui.components.PlaylistPanel
import com.rero.miozicplayer.ui.theme.CarDimensions
import com.rero.miozicplayer.ui.theme.MiozicTheme

@Composable
fun MainScreen(
    viewModel: PlayerViewModel,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MiozicTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(WindowInsets.systemBars)
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.backgroundGradientStart, colors.backgroundGradientEnd),
                ),
            ),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            PlaylistPanel(
                title = when (state.currentNav) {
                    NavDestination.BLUETOOTH -> "Bluetooth Player"
                    NavDestination.BROWSE -> state.openedBrowseFolder?.title ?: "Browse"
                },
                description = when (state.currentNav) {
                    NavDestination.BLUETOOTH -> state.bluetoothDeviceName?.let { "Connected to $it" }
                        ?: "Connect your phone in system Bluetooth settings, then play music on your phone."
                    NavDestination.BROWSE -> state.openedBrowseFolder?.description
                        ?: "Open a folder to browse songs on this device."
                },
                songs = when (state.currentNav) {
                    NavDestination.BLUETOOTH -> emptyList()
                    NavDestination.BROWSE -> state.browseDisplaySongs
                },
                currentIndex = highlightedIndex(state),
                onSongClick = viewModel::playSongAt,
                isDarkMode = isDarkMode,
                onThemeToggle = onThemeToggle,
                browseFolders = if (state.currentNav == NavDestination.BROWSE) state.browseFolders else emptyList(),
                browseOpenedFolderId = if (state.currentNav == NavDestination.BROWSE) {
                    state.browseOpenedFolderId
                } else {
                    null
                },
                onFolderClick = viewModel::openBrowseFolder,
                onBrowseBack = viewModel::closeBrowseFolder,
                modifier = Modifier.weight(CarDimensions.playlistPanelWeight),
            )

            NowPlayingPanel(
                songs = state.songs,
                currentIndex = state.currentIndex,
                isPlaying = state.isPlaying,
                isShuffleOn = state.isShuffleOn,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                onShuffle = viewModel::toggleShuffle,
                onPrevious = viewModel::skipToPrevious,
                onRewind15 = viewModel::seekBackward15,
                onPlayPause = viewModel::togglePlayPause,
                onForward15 = viewModel::seekForward15,
                onNext = viewModel::skipToNext,
                onSeek = viewModel::seekTo,
                isBluetoothMode = state.isBluetoothMode,
                displayTitle = state.bluetoothTitle,
                displayArtist = state.bluetoothArtist,
                displayAlbumArtUri = state.bluetoothAlbumArtUri,
                isBluetoothConnected = state.isBluetoothConnected,
                modifier = Modifier.weight(CarDimensions.playerPanelWeight),
            )

            NavSidebar(
                currentNav = state.currentNav,
                onNavigate = viewModel::navigateTo,
                isBluetoothConnected = state.isBluetoothConnected,
            )
        }
    }
}

private fun highlightedIndex(state: com.rero.miozicplayer.player.PlayerState): Int {
    val displaySongs = when (state.currentNav) {
        NavDestination.BLUETOOTH -> emptyList()
        NavDestination.BROWSE -> state.browseDisplaySongs
    }
    val currentSong = state.songs.getOrNull(state.currentIndex) ?: return state.currentIndex
    return displaySongs.indexOfFirst { it.uri == currentSong.uri }
        .takeIf { it >= 0 }
        ?: state.currentIndex
}
