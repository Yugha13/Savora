package com.pennywiseai.tracker.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.rounded.Home
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
        selectedIcon = Icons.Rounded.Home
    )
    
    data object Analytics : BottomNavItem(
        route = "analytics",
        title = "Analytics",
        icon = Icons.Outlined.SentimentSatisfied,
        selectedIcon = Icons.Outlined.SentimentSatisfied // Filled face looks weird, outlined is fine
    )
    
    data object Categories : BottomNavItem(
        route = "categories",
        title = "Categories",
        icon = Icons.Outlined.Category,
        selectedIcon = Icons.Outlined.Category
    )
    
    data object Chat : BottomNavItem(
        route = "chat",
        title = "Chat",
        icon = Icons.Outlined.MailOutline,
        selectedIcon = Icons.Outlined.MailOutline
    )
}