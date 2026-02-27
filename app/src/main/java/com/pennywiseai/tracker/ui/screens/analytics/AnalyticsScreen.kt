package com.pennywiseai.tracker.ui.screens.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.presentation.common.TimePeriod
import com.pennywiseai.tracker.presentation.common.TransactionTypeFilter
import com.pennywiseai.tracker.ui.components.*
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.DateRangeUtils
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: (category: String?, merchant: String?, period: String?, currency: String?) -> Unit = { _, _, _, _ -> },
    onNavigateToHome: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactionTypeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()
    val isUnifiedMode by viewModel.isUnifiedMode.collectAsStateWithLifecycle()
    
    var showAdvancedFilters by rememberSaveable { mutableStateOf(false) }
    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }

    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val activeFilterCount = if (transactionTypeFilter != TransactionTypeFilter.EXPENSE) 1 else 0

    val timePeriods = remember { TimePeriod.values().toList().take(4) } // Limit to 4 for the pill selector UI
    val customRangeLabel = remember(customDateRange) {
        DateRangeUtils.formatDateRange(customDateRange)
    }

    val isDark = isSystemInDarkTheme()
    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = 40.dp,
                bottom = Dimensions.Component.bottomBarHeight + Spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Period Selector (Pill style)
            item {
                CustomPeriodSelector(
                    periods = timePeriods,
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { period ->
                        if (period == TimePeriod.CUSTOM) {
                            showDateRangePicker = true
                        } else {
                            viewModel.selectPeriod(period)
                        }
                    }
                )
            }

            // Analytics Summary Card (Dark styling)
            if (uiState.totalSpending > BigDecimal.ZERO || uiState.transactionCount > 0) {
                item {
                    AnalyticsSummaryCard(
                        totalAmount = uiState.totalSpending,
                        transactionCount = uiState.transactionCount,
                        averageAmount = uiState.averageAmount,
                        topCategory = uiState.topCategory,
                        topCategoryPercentage = uiState.topCategoryPercentage,
                        currency = uiState.currency,
                        isLoading = uiState.isLoading
                    )
                }
            }

            // Currency Selector (if multiple currencies available and not in unified mode)
            if (availableCurrencies.size > 1 && !isUnifiedMode) {
                item {
                    CurrencyFilterRow(
                        selectedCurrency = selectedCurrency,
                        availableCurrencies = availableCurrencies,
                        onCurrencySelected = { viewModel.selectCurrency(it) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }

            // Spending Progress Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spending progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "${uiState.transactionCount} entries tracked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            // Bar Chart (Derived from Category Data)
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    CategoryBarChart(
                        categories = uiState.categoryBreakdown.take(6), // up to 6 bars
                        currency = selectedCurrency,
                        averageAmount = uiState.averageAmount
                    )
                }
            }
            
            // Collapsible Transaction Type Filter
            item {
                CollapsibleFilterRow(
                    isExpanded = showAdvancedFilters,
                    activeFilterCount = activeFilterCount,
                    onToggle = { showAdvancedFilters = !showAdvancedFilters },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(TransactionTypeFilter.values().toList()) { typeFilter ->
                            FilterChip(
                                selected = transactionTypeFilter == typeFilter,
                                onClick = { viewModel.setTransactionTypeFilter(typeFilter) },
                                label = { Text(typeFilter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Category Breakdown Section (List format, existing but restyled)
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    CategoryBreakdownCard(
                        categories = uiState.categoryBreakdown,
                        currency = selectedCurrency,
                        onCategoryClick = { category ->
                            onNavigateToTransactions(category.name, null, selectedPeriod.name, selectedCurrency)
                        }
                    )
                }
            }

            // Top Merchants Section
            if (uiState.topMerchants.isNotEmpty()) {
                item {
                    SectionHeader(title = "Top Merchants")
                }
                item {
                    ExpandableList(
                        items = uiState.topMerchants,
                        visibleItemCount = 3,
                        modifier = Modifier.fillMaxWidth()
                    ) { merchant ->
                        MerchantListItem(
                            merchant = merchant,
                            currency = selectedCurrency,
                            onClick = {
                                onNavigateToTransactions(null, merchant.name, selectedPeriod.name, selectedCurrency)
                            }
                        )
                    }
                }
            }

            // Empty state
            if (uiState.topMerchants.isEmpty() && uiState.categoryBreakdown.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyAnalyticsState(onScanSmsClick = onNavigateToHome)
                }
            }
        }
    }

    if (showDateRangePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onConfirm = { startDate, endDate ->
                viewModel.setCustomDateRange(startDate, endDate)
                showDateRangePicker = false
            },
            initialStartDate = customDateRange?.first,
            initialEndDate = customDateRange?.second
        )
    }
}

@Composable
fun CustomPeriodSelector(
    periods: List<TimePeriod>,
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F6FA)
    val activeBg = Color(0xFFED7D5C) // Orange from image
    val activeText = Color.White
    val inactiveText = if (isDark) Color(0xFFAAAAAA) else Color(0xFF888888)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(containerBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        periods.forEach { period ->
            val isSelected = period == selectedPeriod
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (isSelected) activeBg else Color.Transparent)
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label.split(" ").firstOrNull() ?: period.label, // simplify label
                    color = if (isSelected) activeText else inactiveText,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CategoryBarChart(
    categories: List<CategoryData>,
    currency: String,
    averageAmount: BigDecimal
) {
    val isDark = isSystemInDarkTheme()
    val chartBg = if (isDark) Color(0xFF22262E) else Color(0xFFF0F4FA)
    val inactiveBarColor = Color(0xFFC9E265) // Light Green
    val activeBarColor = Color(0xFFF26E50) // Orange
    val dashLineColor = if (isDark) Color(0xFF555555) else Color(0xFFAAAAAA)
    val textColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF888888)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(chartBg)
            .padding(16.dp)
            .height(280.dp) // Fixed height for chart
    ) {
        val maxAmount = categories.maxOfOrNull { it.amount.toFloat() } ?: 100f
        val ySteps = 5
        
        Row(modifier = Modifier.fillMaxSize()) {
            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // Generate step labels
                for (i in ySteps downTo 0) {
                    val stepVal = if (maxAmount == 0f) 0f else (maxAmount / ySteps) * i
                    Text(
                        text = CurrencyFormatter.formatCurrency(BigDecimal(stepVal.toString()), currency).replace(".00", ""),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                }
            }

            // Chart Drawing Area
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val chartWidth = size.width
                    val chartHeight = size.height - 24.dp.toPx() // Leave room for X-axis
                    
                    // Draw horizontal dashed lines
                    for (i in 0..ySteps) {
                        val y = chartHeight - (chartHeight / ySteps) * i
                        drawLine(
                            color = dashLineColor.copy(alpha = 0.3f),
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Average Amount Dashed line
                    val avgValue = averageAmount.toFloat()
                    if (avgValue in 0f..maxAmount) {
                        val avgY = chartHeight - (avgValue / maxAmount) * chartHeight
                        drawLine(
                            color = dashLineColor,
                            start = Offset(0f, avgY),
                            end = Offset(chartWidth, avgY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                    }

                    // Draw Bars
                    if (categories.isNotEmpty()) {
                        val barCount = categories.size
                        val barSpacing = chartWidth / (barCount * 1.5f)
                        val barWidth = barSpacing * 0.8f
                        val totalOccupiedWidth = barCount * barSpacing
                        val startOffsetX = (chartWidth - totalOccupiedWidth) / 2f

                        // Find the index of the highest category to select it
                        var selectedIdx = 0
                        var maxAmt = -1f
                        categories.forEachIndexed { idx, cat -> 
                            if(cat.amount.toFloat() > maxAmt) { maxAmt = cat.amount.toFloat(); selectedIdx = idx }
                        }

                        categories.forEachIndexed { index, category ->
                            val isSelected = index == selectedIdx
                            val barHeight = if (maxAmount == 0f) 0f else (category.amount.toFloat() / maxAmount) * chartHeight
                            
                            val xPos = startOffsetX + (index * barSpacing) + (barSpacing / 2f) - (barWidth / 2f)
                            val yPos = chartHeight - barHeight

                            // Draw Bar
                            drawRoundRect(
                                color = if (isSelected) activeBarColor else inactiveBarColor,
                                topLeft = Offset(xPos, yPos),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                            )

                            // Draw Circle for selected on the average line? Or max height?
                            // Image shows a white dot on the selected bar at the dashed line level.
                            if (isSelected) {
                                val avgY = chartHeight - (avgValue / maxAmount) * chartHeight
                                drawCircle(
                                    color = Color.White,
                                    radius = barWidth * 0.2f,
                                    center = Offset(xPos + barWidth/2f, if(avgValue > 0) avgY else yPos)
                                )
                                // Stroke around circle
                                drawCircle(
                                    color = activeBarColor,
                                    radius = barWidth * 0.2f,
                                    center = Offset(xPos + barWidth/2f, if(avgValue > 0) avgY else yPos),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // X-Axis Labels (Category names)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp), // Adjust padding based on width
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    categories.forEach { category ->
                        Text(
                            text = category.name.take(3), // Abbreviate
                            color = textColor,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun MerchantListItem(
    merchant: MerchantData,
    currency: String,
    onClick: () -> Unit = {}
) {
    val subtitle = buildString {
        append("${merchant.transactionCount} ")
        append(if (merchant.transactionCount == 1) "transaction" else "transactions")
        if (merchant.isSubscription) {
            append(" • Subscription")
        }
    }
    
    ListItemCard(
        leadingContent = {
            BrandIcon(
                merchantName = merchant.name,
                size = 40.dp,
                showBackground = true
            )
        },
        title = merchant.name,
        subtitle = subtitle,
        amount = CurrencyFormatter.formatCurrency(merchant.amount, currency),
        onClick = onClick
    )
}

@Composable
private fun EmptyAnalyticsState(
    onScanSmsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.Padding.content),
        contentAlignment = Alignment.Center
    ) {
        PennyWiseCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.empty),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = "No spending data yet",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Scan your SMS to see spending insights",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Button(
                    onClick = onScanSmsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Scan SMS")
                }
            }
        }
    }
}

@Composable
private fun CurrencyFilterRow(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Text(
                text = "Currency:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    vertical = Spacing.sm,
                    horizontal = Spacing.xs
                )
            )
        }
        items(availableCurrencies) { currency ->
            FilterChip(
                selected = selectedCurrency == currency,
                onClick = { onCurrencySelected(currency) },
                label = { Text(currency) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}