package com.rero.miozicplayer.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private val themeColorSpec = tween<Color>(
    durationMillis = 550,
    easing = FastOutSlowInEasing,
)

@Composable
fun rememberAnimatedMiozicColors(darkTheme: Boolean): MiozicColorScheme {
    val target = if (darkTheme) DarkMiozicColors else LightMiozicColors

    val backgroundGradientStart by animateColorAsState(
        targetValue = target.backgroundGradientStart,
        animationSpec = themeColorSpec,
        label = "backgroundGradientStart",
    )
    val backgroundGradientEnd by animateColorAsState(
        targetValue = target.backgroundGradientEnd,
        animationSpec = themeColorSpec,
        label = "backgroundGradientEnd",
    )
    val accent by animateColorAsState(
        targetValue = target.accent,
        animationSpec = themeColorSpec,
        label = "accent",
    )
    val textPrimary by animateColorAsState(
        targetValue = target.textPrimary,
        animationSpec = themeColorSpec,
        label = "textPrimary",
    )
    val textSecondary by animateColorAsState(
        targetValue = target.textSecondary,
        animationSpec = themeColorSpec,
        label = "textSecondary",
    )
    val textTertiary by animateColorAsState(
        targetValue = target.textTertiary,
        animationSpec = themeColorSpec,
        label = "textTertiary",
    )
    val card by animateColorAsState(
        targetValue = target.card,
        animationSpec = themeColorSpec,
        label = "card",
    )
    val pillBackground by animateColorAsState(
        targetValue = target.pillBackground,
        animationSpec = themeColorSpec,
        label = "pillBackground",
    )
    val progressTrack by animateColorAsState(
        targetValue = target.progressTrack,
        animationSpec = themeColorSpec,
        label = "progressTrack",
    )
    val navInactive by animateColorAsState(
        targetValue = target.navInactive,
        animationSpec = themeColorSpec,
        label = "navInactive",
    )
    val albumPlaceholder by animateColorAsState(
        targetValue = target.albumPlaceholder,
        animationSpec = themeColorSpec,
        label = "albumPlaceholder",
    )
    val albumPlaceholderGradientStart by animateColorAsState(
        targetValue = target.albumPlaceholderGradientStart,
        animationSpec = themeColorSpec,
        label = "albumPlaceholderGradientStart",
    )
    val albumPlaceholderGradientEnd by animateColorAsState(
        targetValue = target.albumPlaceholderGradientEnd,
        animationSpec = themeColorSpec,
        label = "albumPlaceholderGradientEnd",
    )
    val controlBar by animateColorAsState(
        targetValue = target.controlBar,
        animationSpec = themeColorSpec,
        label = "controlBar",
    )
    val activeSongCard by animateColorAsState(
        targetValue = target.activeSongCard,
        animationSpec = themeColorSpec,
        label = "activeSongCard",
    )
    val shadow by animateColorAsState(
        targetValue = target.shadow,
        animationSpec = themeColorSpec,
        label = "shadow",
    )

    return MiozicColorScheme(
        backgroundGradientStart = backgroundGradientStart,
        backgroundGradientEnd = backgroundGradientEnd,
        accent = accent,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textTertiary = textTertiary,
        card = card,
        pillBackground = pillBackground,
        progressTrack = progressTrack,
        navInactive = navInactive,
        albumPlaceholder = albumPlaceholder,
        albumPlaceholderGradientStart = albumPlaceholderGradientStart,
        albumPlaceholderGradientEnd = albumPlaceholderGradientEnd,
        controlBar = controlBar,
        activeSongCard = activeSongCard,
        shadow = shadow,
    )
}
