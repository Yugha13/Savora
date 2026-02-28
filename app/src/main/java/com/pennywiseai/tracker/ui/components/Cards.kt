package com.pennywiseai.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip

/**
 * Base card component with consistent styling
 */
@Composable
fun PennyWiseCard(
    modifier: Modifier = Modifier,
    containerColor: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier,
            onClick = onClick,
            colors = containerColor
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            colors = containerColor
        ) {
            content()
        }
    }
}

/**
 * Summary card for displaying large amounts with optional subtitle
 * Used for: Month summary, Total subscriptions, etc.
 */
@Composable
fun SummaryCard(
    title: String,
    amount: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    containerColor: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ),
    amountColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: (() -> Unit)? = null
) {
    PennyWiseCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = containerColor,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.card),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = Dimensions.Alpha.subtitle)
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = Dimensions.Alpha.surface),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Modern dashboard summary card based on custom layout
 * Features top-left icon, top-right action button inside a concave cutout, and bottom pill
 */
@Composable
fun DashboardSummaryCard(
    title: String,
    amount: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color,
    amountColor: androidx.compose.ui.graphics.Color = Color.White,
    titleColor: androidx.compose.ui.graphics.Color = Color.White,
    icon: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val screenBg = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp) // Taller to match the majestic feel
            .clip(RoundedCornerShape(32.dp))
            .clickable { onClick?.invoke() }
    ) {
        // Main Background with subtle gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            containerColor.copy(alpha = 0.9f),
                            containerColor
                        )
                    )
                )
        )
        
        // Background color cutout effect (The concave curve trick)
        // By placing a box of the same color as the screen background at the top right,
        // it visually "cuts out" the rounded card corner.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 36.dp, y = (-36).dp)
                .size(100.dp)
                .background(screenBg, CircleShape)
        )
        
        // Top Right Custom Cutout Action Button (The floating orange button)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = 8.dp) // Just inside the visual cutout
                .size(48.dp)
                .background(Color(0xFFE2895E), CircleShape), // Soft orange
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.OpenInFull, // Fits the expand/navigate action
                contentDescription = "Details",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Inner Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp), // Generous padding
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section (Icon)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Middle section (Title & Amount)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Five stars like in the mockup, next to the "Rating / Amount"
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Val: $amount", // Mimicking "Rating: 4.5" visually
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColor.copy(alpha = 0.95f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Bottom section (Subtitle Pill & Action Icon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Subtitle Pill
                if (subtitle != null) {
                    Row(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = titleColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = titleColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 160.dp) // Prevent pill from squishing chat bubble
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Right Action Icon (Chat bubble from mockup)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, titleColor.copy(alpha = 0.2f), CircleShape)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        tint = titleColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * List item card for transactions, subscriptions, etc.
 */
@Composable
fun ListItemCard(
    title: String,
    subtitle: String,
    amount: String,
    modifier: Modifier = Modifier,
    amountColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    PennyWiseCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(modifier = Modifier.width(Spacing.md))
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (trailingContent != null) {
                trailingContent()
            } else {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor
                )
            }
        }
    }
}

/**
 * Section header component
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (action != null) {
            action()
        }
    }
}