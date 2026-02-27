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

            // Analytics Summary Card (Restored original with Filter)
            if (uiState.totalSpending > BigDecimal.ZERO || uiState.transactionCount > 0) {
                item {
                    AnalyticsSummaryCard(
                        totalAmount = uiState.totalSpending,
                        transactionCount = uiState.transactionCount,
                        currency = uiState.currency,
                        isLoading = uiState.isLoading
                    )
                }
                
                // Add Spacing
                item { Spacer(modifier = Modifier.height(16.dp)) }
                
                // Category Pie Chart
                item {
                    CategoryPieChartCard(
                        totalAmount = uiState.totalSpending,
                        categories = uiState.categoryBreakdown,
                        currency = uiState.currency,
                        currentFilter = transactionTypeFilter,
                        onFilterSelected = { viewModel.setTransactionTypeFilter(it) }
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

            // Category Breakdown Section (Grid format)
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    CategoryGridCards(
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
    val containerBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val activeBg = Color(0xFFED7D5C) // Orange from image
    val activeText = Color.White
    val inactiveText = if (isDark) Color(0xFFAAAAAA) else Color(0xFF666666)

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
fun CategoryGridCards(
    categories: List<CategoryData>,
    currency: String,
    onCategoryClick: (CategoryData) -> Unit
) {
    val chunkedCategories = categories.chunked(2)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        chunkedCategories.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { category ->
                    CategoryGridItemCard(
                        category = category,
                        currency = currency,
                        onClick = { onCategoryClick(category) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CategoryGridItemCard(
    category: CategoryData,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF9F9F9)
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryText = if (isDark) Color(0xFFAAAAAA) else Color(0xFF666666)
    val iconBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFEBEBEB)
    val pillBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE0E0E0)
    
    Card(
        onClick = onClick,
        modifier = modifier.height(150.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryMapping.categories[category.name]?.icon ?: Icons.Default.Category,
                        contentDescription = category.name,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Pill
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(pillBg).padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${category.transactionCount} Items",
                        color = secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp
                    )
                }
            }
            
            Column {
                Text(
                    text = category.name,
                    color = secondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CurrencyFormatter.formatCurrency(category.amount, currency),
                    color = textColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
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