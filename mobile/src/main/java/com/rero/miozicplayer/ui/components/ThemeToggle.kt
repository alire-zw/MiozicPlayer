package com.rero.miozicplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.rero.miozicplayer.ui.theme.CarDimensions
import com.rero.miozicplayer.ui.theme.MiozicTheme

private val toggleShape = RoundedCornerShape(16.dp)
private val indicatorShape = RoundedCornerShape(14.dp)
private val themeAnimSpec = tween<Float>(durationMillis = 450, easing = FastOutSlowInEasing)
private val themeColorAnimSpec = tween<androidx.compose.ui.graphics.Color>(
    durationMillis = 450,
    easing = FastOutSlowInEasing,
)

@Composable
fun ThemeToggle(
    isDarkMode: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiozicTheme.colors
    val buttonSize = CarDimensions.themeToggleButtonSize
    val gap = 6.dp

    val indicatorOffset by animateDpAsState(
        targetValue = if (isDarkMode) buttonSize + gap else 0.dp,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "themeIndicatorOffset",
    )
    val trackColor by animateColorAsState(
        targetValue = colors.pillBackground.copy(alpha = 0.7f),
        animationSpec = themeColorAnimSpec,
        label = "themeTrackColor",
    )
    val indicatorColor by animateColorAsState(
        targetValue = colors.card,
        animationSpec = themeColorAnimSpec,
        label = "themeIndicatorColor",
    )

    Box(
        modifier = modifier
            .clip(toggleShape)
            .background(trackColor)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .size(buttonSize)
                .clip(indicatorShape)
                .background(indicatorColor),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            ThemeIconButton(
                iconRes = MiozicIcons.Sun,
                contentDescription = "Light mode",
                isSelected = !isDarkMode,
                onClick = { onToggle(false) },
            )
            ThemeIconButton(
                iconRes = MiozicIcons.Moon,
                contentDescription = "Dark mode",
                isSelected = isDarkMode,
                onClick = { onToggle(true) },
            )
        }
    }
}

@Composable
private fun ThemeIconButton(
    iconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MiozicTheme.colors

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) colors.accent else colors.navInactive,
        animationSpec = themeColorAnimSpec,
        label = "themeIconTint",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 0.92f,
        animationSpec = themeAnimSpec,
        label = "themeIconScale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.65f,
        animationSpec = themeAnimSpec,
        label = "themeIconAlpha",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(CarDimensions.themeToggleButtonSize)
            .scale(iconScale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        MiozicIcon(
            iconRes = iconRes,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier
                .size(CarDimensions.themeToggleIconSize)
                .alpha(iconAlpha),
        )
    }
}
