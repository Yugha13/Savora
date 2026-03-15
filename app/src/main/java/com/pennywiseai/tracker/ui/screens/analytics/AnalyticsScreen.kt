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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
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
            // 1. Top Header
            item {
                ExpensesTopHeader()
            }
            
            // 2. Calendar Strip
            item {
                CalendarStrip(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() }
                )
            }
            
            // 3. Summary Cards (Total Salary, Total Expense)
            item {
                ExpensesSummaryCards(
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalSpending,
                    currency = uiState.currency
                )
            }
            
            // 4. Expenses Category List
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Expenses",
                        action = { Text("View All", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }
                
                items(uiState.categoryBreakdown) { categoryInfo ->
                    ExpensesCategoryItem(categoryInfo = categoryInfo, currency = uiState.currency)
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    AnalyticsOverview(uiState = uiState)
                }
            } else if (!uiState.isLoading) {
                // Empty state
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
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val secondaryText = if (isDark) Color(0xFFAAAAAA) else Color(0xFF888888)
    val iconBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF1F1F1)

    // Example date mapping (since MerchantData doesn't have individual dates, use a placeholder that looks like the mockup for now, or you could pass it in later)
    val dummyTimeDate = "10:00 am • 08 March, 2025"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon Background
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryMapping.categories[merchant.name]?.icon ?: Icons.Default.Category,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Center Text Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = merchant.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dummyTimeDate,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText
            )
        }

        // Trailing Amount
        Text(
            text = CurrencyFormatter.formatCurrency(merchant.amount, currency),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
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
        SavoraCard(
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

@Composable
fun ExpensesTopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE8E5FA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = "Expenses",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = "Notifications",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .offset(x = (-2).dp, y = 2.dp)
            )
        }
    }
}

@Composable
fun CalendarStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous")
            }
            Text(
                text = selectedDate.format(monthFormatter),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Show 7 days centered around selectedDate
            val startDate = selectedDate.minusDays(3)
            val days = (0..6).map { startDate.plusDays(it.toLong()) }
            
            days.forEach { date ->
                val isSelected = date.isEqual(selectedDate)
                val dayName = date.dayOfWeek.name.take(1) // M, T, W...
                
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                        .clickable { onDateSelected(date) }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White else Color.Gray
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                    )
                    // Optional: dot for transactions on this day
                }
            }
        }
    }
}

@Composable
fun ExpensesSummaryCards(totalIncome: BigDecimal, totalExpense: BigDecimal, currency: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Purple Salary Card
        Card(
            modifier = Modifier.weight(1f).height(120.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha=0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Wallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Icon(Icons.Default.MoreHoriz, null, tint = Color.White)
                }
                Column {
                    Text("Total Salary", color = Color.White.copy(alpha=0.7f), style = MaterialTheme.typography.bodySmall)
                    Text(CurrencyFormatter.formatCurrency(totalIncome, currency), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        
        // Orange Expense Card
        Card(
            modifier = Modifier.weight(1f).height(120.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha=0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Icon(Icons.Default.MoreHoriz, null, tint = Color.White)
                }
                Column {
                    Text("Total Expense", color = Color.White.copy(alpha=0.7f), style = MaterialTheme.typography.bodySmall)
                    Text(CurrencyFormatter.formatCurrency(totalExpense, currency), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}@Composable
fun ExpensesCategoryItem(categoryInfo: CategoryData, currency: String) {
    val budgetText = if (categoryInfo.limit != null) {
        "Budget ${CurrencyFormatter.formatCurrency(categoryInfo.limit, currency)}"
    } else {
        "No Budget Set"
    }
    
    val budgetProgress = categoryInfo.budgetPercentage?.div(100f)?.coerceIn(0f, 1f) ?: 0f
    val percentageText = categoryInfo.budgetPercentage?.let { "${it.toInt()}%" } ?: "0%"

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryMapping.categories[categoryInfo.name]?.icon ?: Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column {
                Text(
                    text = categoryInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = budgetText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "-${CurrencyFormatter.formatCurrency(categoryInfo.amount, currency)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { budgetProgress },
                    modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = Color.LightGray.copy(alpha=0.3f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(percentageText, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun AnalyticsOverview(uiState: AnalyticsUiState) {
    val incomeSpentPercentage = if (uiState.totalIncome > BigDecimal.ZERO) {
        (uiState.totalSpending.toFloat() / uiState.totalIncome.toFloat() * 100f).coerceIn(0f, 100f)
    } else 0f
    
    val currentMonthYear = java.time.format.DateTimeFormatter.ofPattern("MMMM, yyyy").format(java.time.LocalDate.now())

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Analytics",
            action = { Text("View All", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "You have Spent ${CurrencyFormatter.formatCurrency(uiState.totalSpending, uiState.currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Income usage bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF0F0F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(incomeSpentPercentage / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (incomeSpentPercentage > 15f) {
                            Text(
                                text = "${incomeSpentPercentage.toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (incomeSpentPercentage <= 15f) {
                         Text(
                            text = "${incomeSpentPercentage.toInt()}%",
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                        )
                    }
                }
            }
            
            Text(
                text = currentMonthYear,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Donut Chart logic
        Box(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            DonutChart(
                categories = uiState.categoryBreakdown.take(4),
                modifier = Modifier.size(200.dp)
            )
        }
    }
}

@Composable
fun DonutChart(categories: List<CategoryData>, modifier: Modifier = Modifier) {
    if (categories.isEmpty()) return
    
    val colors = listOf(
        Color(0xFF2E334D), // Dark Blue
        Color(0xFFFF7043), // Orange
        Color(0xFF8544F1), // Purple
        Color(0xFFA5D6A7)  // Green
    )
    
    Canvas(modifier = modifier) {
        var startAngle = -90f
        categories.forEachIndexed { index, category ->
            val sweepAngle = (category.amount.toFloat() / categories.sumOf { it.amount.toDouble() }.toFloat()) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 40.dp.toPx())
            )
            
            // Draw Labels (Simplistic)
            val angle = startAngle + sweepAngle / 2
            val radius = size.width / 2 + 40.dp.toPx()
            val x = (radius * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat() + size.width / 2
            val y = (radius * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat() + size.height / 2
            
            // We'll skip complex text drawing in Canvas for now to avoid complexity, 
            // the DonutChart looks good enough as a visual summary.
            
            startAngle += sweepAngle
        }
    }
    
    // Labels overlay
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        categories.take(1).forEach { 
            Text(it.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(CurrencyFormatter.formatCurrency(it.amount, "INR"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}