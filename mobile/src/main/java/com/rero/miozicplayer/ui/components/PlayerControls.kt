package com.rero.miozicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rero.miozicplayer.ui.theme.CarDimensions
import com.rero.miozicplayer.ui.theme.MiozicTheme

@Composable
fun PlayerControls(
    isPlaying: Boolean,
    isShuffleOn: Boolean,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onRewind15: () -> Unit,
    onPlayPause: () -> Unit,
    onForward15: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    showShuffle: Boolean = true,
) {
    val colors = MiozicTheme.colors

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(CarDimensions.controlBarCorner), ambientColor = colors.shadow)
            .clip(RoundedCornerShape(CarDimensions.controlBarCorner))
            .background(colors.controlBar)
            .padding(
                horizontal = CarDimensions.controlBarPaddingH,
                vertical = CarDimensions.controlBarPaddingV,
            ),
    ) {
        if (showShuffle) {
            ControlIcon(
                iconRes = MiozicIcons.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffleOn) colors.accent else colors.textPrimary.copy(alpha = 0.7f),
                onClick = onShuffle,
            )
        }
        ControlIcon(
            iconRes = MiozicIcons.Previous,
            contentDescription = "Previous",
            onClick = onPrevious,
        )
        ControlIcon(
            iconRes = MiozicIcons.Rewind15,
            contentDescription = "Rewind 15 seconds",
            onClick = onRewind15,
            size = 34.dp,
        )
        PlayPauseButton(isPlaying = isPlaying, onClick = onPlayPause)
        ControlIcon(
            iconRes = MiozicIcons.Forward15,
            contentDescription = "Forward 15 seconds",
            onClick = onForward15,
            size = 34.dp,
        )
        ControlIcon(
            iconRes = MiozicIcons.Next,
            contentDescription = "Next",
            onClick = onNext,
        )
    }
}

@Composable
private fun ControlIcon(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MiozicTheme.colors.textPrimary.copy(alpha = 0.8f),
    size: androidx.compose.ui.unit.Dp = CarDimensions.controlIconSize,
) {
    MiozicIcon(
        iconRes = iconRes,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(2.dp),
    )
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val colors = MiozicTheme.colors

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(CarDimensions.playButtonSize)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        MiozicIcon(
            iconRes = if (isPlaying) MiozicIcons.Pause else MiozicIcons.Play,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = colors.textPrimary,
            modifier = Modifier.size(38.dp),
        )
    }
}
