package com.rero.miozicplayer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class MiozicColorScheme(
    val backgroundGradientStart: Color,
    val backgroundGradientEnd: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val card: Color,
    val pillBackground: Color,
    val progressTrack: Color,
    val navInactive: Color,
    val albumPlaceholder: Color,
    val albumPlaceholderGradientStart: Color,
    val albumPlaceholderGradientEnd: Color,
    val controlBar: Color,
    val activeSongCard: Color,
    val shadow: Color,
)

val LightMiozicColors = MiozicColorScheme(
    backgroundGradientStart = Color(0xFFE6F0FF),
    backgroundGradientEnd = Color(0xFFFFFFFF),
    accent = Color(0xFF4A90E2),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF666666),
    textTertiary = Color(0xFF999999),
    card = Color(0xFFFFFFFF),
    pillBackground = Color(0xFFF2F2F2),
    progressTrack = Color(0xFFE0E0E0),
    navInactive = Color(0xFF888888),
    albumPlaceholder = Color(0xFFE8E8E8),
    albumPlaceholderGradientStart = Color(0xFF6BA8EE),
    albumPlaceholderGradientEnd = Color(0xFFB8D8FA),
    controlBar = Color(0xFFFFFFFF),
    activeSongCard = Color(0xFFFFFFFF),
    shadow = Color(0x1A000000),
)

val DarkMiozicColors = MiozicColorScheme(
    backgroundGradientStart = Color(0xFF1C1C22),
    backgroundGradientEnd = Color(0xFF0E0E12),
    accent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFF2F2F2),
    textSecondary = Color(0xFFB0B0B0),
    textTertiary = Color(0xFF808080),
    card = Color(0xFF2A2A32),
    pillBackground = Color(0xFF3A3A44),
    progressTrack = Color(0xFF44444E),
    navInactive = Color(0xFF777777),
    albumPlaceholder = Color(0xFF33333C),
    albumPlaceholderGradientStart = Color(0xFF454A5E),
    albumPlaceholderGradientEnd = Color(0xFF22242E),
    controlBar = Color(0xFF2A2A32),
    activeSongCard = Color(0xFF2A2A32),
    shadow = Color(0x40000000),
)

val LocalMiozicColors = staticCompositionLocalOf { LightMiozicColors }

object MiozicTheme {
    val colors: MiozicColorScheme
        @androidx.compose.runtime.Composable
        get() = LocalMiozicColors.current
}
