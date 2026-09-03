package com.rero.miozicplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rero.miozicplayer.data.Song
import com.rero.miozicplayer.ui.theme.CarDimensions
import com.rero.miozicplayer.ui.theme.MiozicTheme

@Composable
fun NowPlayingPanel(
    songs: List<Song>,
    currentIndex: Int,
    isPlaying: Boolean,
    isShuffleOn: Boolean,
    positionMs: Long,
    durationMs: Long,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onRewind15: () -> Unit,
    onPlayPause: () -> Unit,
    onForward15: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isBluetoothMode: Boolean = false,
    displayTitle: String = "",
    displayArtist: String = "",
    displayAlbumArtUri: android.net.Uri? = null,
    isBluetoothConnected: Boolean = false,
) {
    val colors = MiozicTheme.colors
    val currentSong = songs.getOrNull(currentIndex)
    val title = if (isBluetoothMode) displayTitle else (currentSong?.title ?: "No Track")
    val artist = if (isBluetoothMode) displayArtist else (currentSong?.artist ?: "")
    val showArt = isBluetoothMode || songs.isNotEmpty()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Spacer(modifier = Modifier.weight(0.12f))

        if (showArt) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.58f)
                    .graphicsLayer { clip = false },
            ) {
                if (isBluetoothMode) {
                    val btSong = Song(
                        id = -1L,
                        title = title,
                        artist = artist,
                        album = "Bluetooth",
                        durationMs = durationMs,
                        uri = android.net.Uri.EMPTY,
                        albumArtUri = displayAlbumArtUri,
                    )
                    AlbumArt(
                        song = btSong,
                        cornerRadius = CarDimensions.albumArtCorner,
                        iconSize = 72.dp,
                        contentDescription = title,
                        modifier = Modifier.size(CarDimensions.albumArtMain),
                    )
                } else {
                    AlbumArtCarousel(
                        songs = songs,
                        currentIndex = currentIndex,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LocaleText(
            text = when {
                isBluetoothMode && !isBluetoothConnected -> "Connect your phone via Bluetooth"
                isBluetoothMode && title.isBlank() -> "Waiting for music..."
                else -> title
            },
            fontSize = CarDimensions.trackTitleSize,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(4.dp))

        LocaleText(
            text = when {
                isBluetoothMode && !isBluetoothConnected -> "Pair iPhone in system Bluetooth settings"
                else -> artist
            },
            fontSize = CarDimensions.trackArtistSize,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        PlayerProgressBar(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeek = onSeek,
            seekEnabled = !isBluetoothMode,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(18.dp))

        PlayerControls(
            isPlaying = isPlaying,
            isShuffleOn = isShuffleOn,
            onShuffle = onShuffle,
            onPrevious = onPrevious,
            onRewind15 = onRewind15,
            onPlayPause = onPlayPause,
            onForward15 = onForward15,
            onNext = onNext,
            showShuffle = !isBluetoothMode,
            modifier = Modifier.fillMaxWidth(0.92f),
        )

        Spacer(modifier = Modifier.weight(0.08f))
    }
}
