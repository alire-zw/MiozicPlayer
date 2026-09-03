package com.rero.miozicplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.rero.miozicplayer.data.Song
import com.rero.miozicplayer.ui.theme.CarDimensions
import com.rero.miozicplayer.ui.theme.MiozicTheme
import kotlin.math.abs

private const val CAROUSEL_DURATION_MS = 380
private const val SIDE_SCALE = 0.82f
private const val SIDE_ALPHA = 0.72f
private const val SIDE_ROTATION_Y = 26f
private const val VISIBLE_SLOT_RANGE = 1.35f

@Composable
fun AlbumArtCarousel(
    songs: List<Song>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MiozicTheme.colors
    val density = LocalDensity.current.density
    val offsetPx = with(LocalDensity.current) { CarDimensions.albumArtOffset.toPx() }
    val centerIndex = remember { Animatable(currentIndex.toFloat()) }

    LaunchedEffect(currentIndex, songs.size) {
        if (songs.isEmpty()) return@LaunchedEffect
        val target = currentIndex.coerceIn(0, songs.lastIndex).toFloat()
        if (abs(centerIndex.value - target) < 0.001f) return@LaunchedEffect
        centerIndex.animateTo(
            targetValue = target,
            animationSpec = tween(CAROUSEL_DURATION_MS, easing = FastOutSlowInEasing),
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { clip = false },
    ) {
        if (songs.isEmpty()) return@Box

        val center by centerIndex.asState()
        val visibleItems = songs.indices
            .map { index -> index to relativeSlot(index, center, songs.size) }
            .filter { (_, slot) -> abs(slot) <= VISIBLE_SLOT_RANGE }
            .sortedByDescending { (_, slot) -> abs(slot) }

        for ((index, slot) in visibleItems) {
            key(songs[index].id) {
                CarouselCover(
                    song = songs[index],
                    slot = slot,
                    offsetPx = offsetPx,
                    density = density,
                    shadowColor = colors.shadow,
                    modifier = Modifier.zIndex(2f - abs(slot)),
                )
            }
        }
    }
}

@Composable
private fun CarouselCover(
    song: Song,
    slot: Float,
    offsetPx: Float,
    density: Float,
    shadowColor: Color,
    modifier: Modifier = Modifier,
) {
    val absSlot = abs(slot)
    val clamped = absSlot.coerceIn(0f, 1f)
    val scale = lerp(1f, SIDE_SCALE, clamped)
    val alpha = when {
        absSlot <= 1f -> lerp(1f, SIDE_ALPHA, absSlot)
        else -> lerp(SIDE_ALPHA, 0f, ((absSlot - 1f) / (VISIBLE_SLOT_RANGE - 1f)).coerceIn(0f, 1f))
    }
    val isCenter = absSlot < 0.1f

    AlbumArt(
        song = song,
        cornerRadius = CarDimensions.albumArtCorner,
        iconSize = 72.dp,
        contentDescription = song.title,
        modifier = modifier
            .size(CarDimensions.albumArtMain)
            .then(
                if (isCenter) {
                    Modifier.shadow(
                        12.dp,
                        RoundedCornerShape(CarDimensions.albumArtCorner),
                        ambientColor = shadowColor,
                    )
                } else {
                    Modifier
                },
            )
            .graphicsLayer {
                translationX = slot * offsetPx
                scaleX = scale
                scaleY = scale
                rotationY = -slot * SIDE_ROTATION_Y
                this.alpha = alpha
                cameraDistance = 14f * density
                transformOrigin = TransformOrigin.Center
            },
    )
}

private fun relativeSlot(index: Int, center: Float, size: Int): Float {
    if (size <= 1) return 0f
    val period = size.toFloat()
    var offset = index - center
    offset -= period * kotlin.math.floor((offset + period / 2f) / period)
    if (offset > period / 2f) offset -= period
    return offset
}
