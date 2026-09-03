package com.rero.miozicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rero.miozicplayer.ui.theme.MiozicTheme

@Composable
fun PlayerProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    seekEnabled: Boolean = true,
) {
    val colors = MiozicTheme.colors
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = formatTime(positionMs),
            fontSize = 13.sp,
            color = colors.textTertiary,
            modifier = Modifier.padding(end = 12.dp),
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .then(
                    if (seekEnabled) {
                        Modifier.pointerInput(durationMs) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek(fraction)
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            val trackWidth = maxWidth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.progressTrack),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.accent),
            )
            Box(
                modifier = Modifier
                    .offset(x = trackWidth * progress.coerceIn(0f, 1f) - 7.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }

        Text(
            text = formatTime(durationMs),
            fontSize = 13.sp,
            color = colors.textTertiary,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
