package com.rero.miozicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rero.miozicplayer.data.NavDestination
import com.rero.miozicplayer.ui.theme.CarDimensions
import com.rero.miozicplayer.ui.theme.MiozicTheme

@Composable
fun NavSidebar(
    currentNav: NavDestination,
    onNavigate: (NavDestination) -> Unit,
    isBluetoothConnected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .width(CarDimensions.sidebarWidth)
            .padding(vertical = 20.dp, horizontal = 6.dp),
    ) {
        NavItem(
            iconRes = if (isBluetoothConnected) MiozicIcons.Bluetooth else MiozicIcons.BluetoothDisconnected,
            label = "Bluetooth",
            isActive = currentNav == NavDestination.BLUETOOTH,
            onClick = { onNavigate(NavDestination.BLUETOOTH) },
        )
        Spacer(modifier = Modifier.height(CarDimensions.navItemSpacing))
        NavItem(
            iconRes = MiozicIcons.Browse,
            label = "Browse",
            isActive = currentNav == NavDestination.BROWSE,
            onClick = { onNavigate(NavDestination.BROWSE) },
        )
    }
}

@Composable
private fun NavItem(
    iconRes: Int,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val colors = MiozicTheme.colors
    val tint = if (isActive) colors.accent else colors.navInactive

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isActive) {
                    Modifier.background(colors.pillBackground.copy(alpha = 0.6f))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        MiozicIcon(
            iconRes = iconRes,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(CarDimensions.navIconSize),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = CarDimensions.navLabelSize,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
        )
    }
}
