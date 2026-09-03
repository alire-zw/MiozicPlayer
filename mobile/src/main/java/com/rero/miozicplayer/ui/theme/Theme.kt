package com.rero.miozicplayer.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue

@Composable
fun MiozicPlayerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val miozicColors = rememberAnimatedMiozicColors(darkTheme)

    val materialScheme = if (darkTheme) {
        darkColorScheme(
            primary = miozicColors.accent,
            onPrimary = DarkMiozicColors.backgroundGradientEnd,
            background = miozicColors.backgroundGradientEnd,
            onBackground = miozicColors.textPrimary,
            surface = miozicColors.card,
            onSurface = miozicColors.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = miozicColors.accent,
            onPrimary = LightMiozicColors.card,
            background = miozicColors.backgroundGradientEnd,
            onBackground = miozicColors.textPrimary,
            surface = miozicColors.card,
            onSurface = miozicColors.textPrimary,
        )
    }

    val animatedPrimary by animateColorAsState(
        targetValue = miozicColors.accent,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "materialPrimary",
    )
    val animatedBackground by animateColorAsState(
        targetValue = miozicColors.backgroundGradientEnd,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "materialBackground",
    )
    val animatedOnBackground by animateColorAsState(
        targetValue = miozicColors.textPrimary,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "materialOnBackground",
    )
    val animatedSurface by animateColorAsState(
        targetValue = miozicColors.card,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "materialSurface",
    )

    val animatedMaterialScheme = materialScheme.copy(
        primary = animatedPrimary,
        background = animatedBackground,
        onBackground = animatedOnBackground,
        surface = animatedSurface,
        onSurface = animatedOnBackground,
    )

    CompositionLocalProvider(LocalMiozicColors provides miozicColors) {
        MaterialTheme(
            colorScheme = animatedMaterialScheme,
            typography = MiozicTypography,
            content = content,
        )
    }
}
