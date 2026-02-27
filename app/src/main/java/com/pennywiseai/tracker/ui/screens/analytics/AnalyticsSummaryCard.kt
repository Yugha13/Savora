package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun AnalyticsSummaryCard(
    totalAmount: BigDecimal,
    categories: List<CategoryData>,
    currency: String,
    currentFilter: TransactionTypeFilter,
    onFilterSelected: (TransactionTypeFilter) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isLoading) 0.5f else 1f,
        animationSpec = tween(300),
        label = "summary_alpha"
    )

    val cardBgColor = Color(0xFF1E1E1E) // Dark styling as per image for both modes
    val textColor = Color.White
    val secondaryTextColor = Color(0xFFAAAAAA)
    
    // Pie chart colors matching the image
    val colorPalette = listOf(
        Color(0xFFC9E265), // Light Green
        Color(0xFFF26E50), // Orange / Earned
        Color(0xFF8C52FF), // Purple / Spent
        Color(0xFFE5B05C), // Yellowish / Savings
        Color(0xFF888888)  // Gray for Others
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(32.dp))
            .background(cardBgColor)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analytics",
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
                
                // Dropdown for Filter in top right corner
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A2A))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Filter",
                            tint = Color.White
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        TransactionTypeFilter.values().forEach { filter ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        filter.label, 
                                        fontWeight = if (filter == currentFilter) FontWeight.Bold else FontWeight.Normal 
                                    ) 
                                },
                                onClick = {
                                    onFilterSelected(filter)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Pie Chart and Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut Chart
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val total = categories.sumOf { it.amount.toDouble() }.toFloat().coerceAtLeast(1f)
                    
                    Canvas(modifier = Modifier.size(130.dp)) {
                        var startAngle = -90f
                        val strokeWidth = 16.dp.toPx()
                        
                        if (categories.isEmpty()) {
                            drawArc(
                                color = Color(0xFF333333),
                                startAngle = startAngle,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        } else {
                            val top4 = categories.take(4)
                            top4.forEachIndexed { index, cat ->
                                val sweepAngle = (cat.amount.toFloat() / total) * 360f
                                drawArc(
                                    color = colorPalette[index % colorPalette.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 8f, // Add small gap
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                            
                            val remaining = categories.drop(4).sumOf { it.amount.toDouble() }.toFloat()
                            if (remaining > 0) {
                                val sweepAngle = (remaining / total) * 360f
                                drawArc(
                                    color = colorPalette.last(),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 8f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                    
                    // Center Text inside Donut
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Balance",
                            color = secondaryTextColor,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.formatCurrency(totalAmount, currency),
                            color = textColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                // Legends (Top 4 Categories)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val top4 = categories.take(4)
                    
                    // Display them in a 2-column format inside the column
                    val chunked = top4.chunked(2)
                    chunked.forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEachIndexed { indexInRow, cat ->
                                val globalIndex = top4.indexOf(cat)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(colorPalette[globalIndex % colorPalette.size])
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat.name,
                                            color = textColor,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(cat.amount, currency).replace(".00", ""),
                                        color = secondaryTextColor,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(start = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}