package com.rero.miozicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.rero.miozicplayer.data.Song
import com.rero.miozicplayer.ui.theme.MiozicColorScheme
import com.rero.miozicplayer.ui.theme.MiozicTheme
import kotlin.math.abs

@Composable
fun AlbumArt(
    song: Song?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    iconSize: Dp = 24.dp,
    contentDescription: String? = null,
) {
    val colors = MiozicTheme.colors
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)
    val artUri = song?.albumArtUri
    val placeholderBrush = rememberPlaceholderBrush(song, colors)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clip(shape),
    ) {
        if (artUri != null) {
            val request = remember(artUri) {
                ImageRequest.Builder(context)
                    .data(artUri)
                    .crossfade(300)
                    .build()
            }
            SubcomposeAsyncImage(
                model = request,
                contentDescription = contentDescription ?: song?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    AlbumArtPlaceholder(
                        brush = placeholderBrush,
                        iconSize = iconSize,
                        colors = colors,
                    )
                },
                error = {
                    AlbumArtPlaceholder(
                        brush = placeholderBrush,
                        iconSize = iconSize,
                        colors = colors,
                    )
                },
            )
        } else {
            AlbumArtPlaceholder(
                brush = placeholderBrush,
                iconSize = iconSize,
                colors = colors,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun AlbumArtPlaceholder(
    brush: Brush,
    iconSize: Dp,
    colors: MiozicColorScheme,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(brush),
    ) {
        MiozicIcon(
            iconRes = MiozicIcons.MusicNote,
            contentDescription = null,
            tint = colors.accent.copy(alpha = 0.55f),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun rememberPlaceholderBrush(song: Song?, colors: MiozicColorScheme): Brush {
    val seed = abs((song?.id ?: song?.title ?: "default").hashCode())
    val variant = seed % 4
    val accentMix = 0.12f + (seed % 100) / 100f * 0.18f

    val (start, end) = when (variant) {
        0 -> colors.albumPlaceholderGradientStart to colors.albumPlaceholderGradientEnd
        1 -> lerp(colors.albumPlaceholderGradientStart, colors.accent, accentMix) to
            colors.albumPlaceholderGradientEnd
        2 -> colors.albumPlaceholderGradientStart to
            lerp(colors.albumPlaceholderGradientEnd, colors.accent, accentMix * 0.7f)
        else -> lerp(colors.albumPlaceholderGradientStart, colors.accent, accentMix * 0.5f) to
            lerp(colors.albumPlaceholderGradientEnd, colors.backgroundGradientStart, accentMix * 0.4f)
    }

    return remember(start, end) {
        Brush.linearGradient(
            colors = listOf(start, lerp(start, end, 0.45f), end),
            start = Offset.Zero,
            end = Offset(800f, 800f),
        )
    }
}
