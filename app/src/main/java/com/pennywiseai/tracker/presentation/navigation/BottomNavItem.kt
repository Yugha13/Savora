package com.pennywiseai.tracker.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
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
    
    data object Expenses : BottomNavItem(
        route = "expenses",
        title = "Expenses",
        icon = Icons.Outlined.AccountBalanceWallet,
        selectedIcon = Icons.Default.AccountBalanceWallet
    )
    
    data object Add : BottomNavItem(
        route = "add",
        title = "Add",
        icon = Icons.Default.Add,
        selectedIcon = Icons.Default.Add
    )
    
    data object Calendar : BottomNavItem(
        route = "calendar",
        title = "Calendar",
        icon = Icons.Outlined.CalendarMonth,
        selectedIcon = Icons.Default.CalendarMonth
    )
    
    data object Profile : BottomNavItem(
        route = "profile",
        title = "Profile",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Default.Person
    )

    data object Analytics : BottomNavItem(
        route = "analytics",
        title = "Analytics",
        icon = Icons.Outlined.BarChart,
        selectedIcon = Icons.Default.BarChart
    )

    data object Chatbot : BottomNavItem(
        route = "chat",
        title = "Chatbot",
        icon = Icons.Outlined.AutoAwesome,
        selectedIcon = Icons.Default.AutoAwesome
    )
}