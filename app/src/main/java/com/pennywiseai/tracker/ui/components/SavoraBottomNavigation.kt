package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pennywiseai.tracker.presentation.navigation.BottomNavItem

@Composable
fun SavoraBottomNavigation(
    navController: NavController,
    onAddClick: () -> Unit = {}
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Expenses,
        BottomNavItem.Add,
        BottomNavItem.Calendar,
        BottomNavItem.Profile
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("?")
    
    val containerColor = MaterialTheme.colorScheme.surface
    val activeColor = MaterialTheme.colorScheme.secondary // Orange
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Nav Bar Background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                .clip(RoundedCornerShape(32.dp))
                .background(containerColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route || 
                    (currentRoute == "analytics" && item == BottomNavItem.Expenses) ||
                    (currentRoute == "settings" && item == BottomNavItem.Profile)
                
                if (item == BottomNavItem.Add) {
                    // Center Add button is drawn via overlay to overlap properly if needed,
                    // but we can put a spacer or handle it here.
                    // Let's just create an empty space of its size here so layout is correct.
                    Spacer(modifier = Modifier.width(48.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        // mapped special navigation or standard route
                                        val routeToNav = when (item) {
                                            BottomNavItem.Expenses -> "analytics"
                                            BottomNavItem.Profile -> "settings"
                                            else -> item.route
                                        }
                                        navController.navigate(routeToNav) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) activeColor else inactiveColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
        
        // Central Add Button Overlay
        Box(
            modifier = Modifier
                .offset(y = (-20).dp) // Pop it slightly above the bar
                .size(56.dp)
                .shadow(8.dp, CircleShape, spotColor = activeColor.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(activeColor)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = BottomNavItem.Add.icon,
                contentDescription = BottomNavItem.Add.title,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}