package com.pennywiseai.tracker.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
) {
    data object Home : BottomNavItem(
        route = "home",
        title = "Home",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Default.Home
    )
    
    data object Analytics : BottomNavItem(
        route = "analytics",
        title = "Analytics",
        icon = Icons.Outlined.Analytics,
        selectedIcon = Icons.Default.Analytics
    )
    
    data object Chat : BottomNavItem(
        route = "chat",
        title = "Chat",
        icon = Icons.AutoMirrored.Outlined.Chat,
        selectedIcon = Icons.AutoMirrored.Filled.Chat
    )
}