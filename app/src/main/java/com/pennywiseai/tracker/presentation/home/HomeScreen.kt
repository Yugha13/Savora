package com.pennywiseai.tracker.presentation.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import android.view.HapticFeedbackConstants
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.work.WorkInfo
import com.pennywiseai.tracker.ui.components.SmsParsingProgressDialog
import kotlinx.coroutines.launch
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.ui.components.SummaryCard
import com.pennywiseai.tracker.ui.components.ListItemCard
import com.pennywiseai.tracker.ui.components.SectionHeader
import com.pennywiseai.tracker.ui.components.SavoraCard
import com.pennywiseai.tracker.ui.components.AccountBalancesCard
import com.pennywiseai.tracker.data.repository.BudgetOverallSummary
import com.pennywiseai.tracker.ui.theme.budget_safe_light
import com.pennywiseai.tracker.ui.theme.budget_safe_dark
import com.pennywiseai.tracker.ui.theme.budget_warning_light
import com.pennywiseai.tracker.ui.theme.budget_warning_dark
import com.pennywiseai.tracker.ui.theme.budget_danger_light
import com.pennywiseai.tracker.ui.theme.budget_danger_dark
import com.pennywiseai.tracker.ui.components.CreditCardsCard
import com.pennywiseai.tracker.ui.components.DashboardSummaryCard
import com.pennywiseai.tracker.ui.components.UnifiedAccountsCard
import com.pennywiseai.tracker.ui.components.spotlightTarget
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.formatAmount
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavController,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToTransactionsWithSearch: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToAddScreen: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onTransactionTypeClick: (String?) -> Unit = {},
    onFabPositioned: (Rect) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val deletedTransaction by viewModel.deletedTransaction.collectAsState()
    val smsScanWorkInfo by viewModel.smsScanWorkInfo.collectAsState()
    val activity = LocalActivity.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // State for full resync confirmation dialog
    var showFullResyncDialog by remember { mutableStateOf(false) }

    // Haptic feedback
    val view = LocalView.current

    // Currency dropdown state
      
    // Check for app updates and reviews when the screen is first displayed
    LaunchedEffect(Unit) {
        // Refresh account balances to ensure proper currency conversion
        viewModel.refreshAccountBalances()

        activity?.let {
            val componentActivity = it as ComponentActivity
            
            // Check for app updates
            viewModel.checkForAppUpdate(
                activity = componentActivity,
                snackbarHostState = snackbarHostState,
                scope = scope
            )
            
            // Check for in-app review eligibility
            viewModel.checkForInAppReview(componentActivity)
        }
    }
    
    // Refresh hidden accounts whenever this screen becomes visible
    // This ensures changes from ManageAccountsScreen are reflected immediately
    DisposableEffect(Unit) {
        viewModel.refreshHiddenAccounts()
        onDispose { }
    }
    
    // Handle delete undo snackbar
    LaunchedEffect(deletedTransaction) {
        deletedTransaction?.let { transaction ->
            // Clear the state immediately to prevent re-triggering
            viewModel.clearDeletedTransaction()
            
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Transaction deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    // Pass the transaction directly since state is already cleared
                    viewModel.undoDeleteTransaction(transaction)
                }
            }
        }
    }
    
    // Clear snackbar when navigating away
    DisposableEffect(Unit) {
        onDispose {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content,
                bottom = Dimensions.Padding.content + 120.dp // Space for dual FABs (Add + Sync)
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 1. Custom Top Header (Profile, Home, Bell)
            item {
                HomeTopHeader()
            }
            
            // 2. Main ATM Card (Expense of this month, Last 4 digits, Bank Name)
            item {
                val expenseThisMonth = uiState.currentMonthExpenses
                val currency = uiState.selectedCurrency
                
                // Get a representative bank name and last 4 if available
                val firstAccount = uiState.accountBalances.firstOrNull()
                val bankName = firstAccount?.bankName ?: "Savora Bank"
                val last4 = firstAccount?.accountLast4 ?: "1965"
                
                MainAtmCard(
                    expenseThisMonth = expenseThisMonth,
                    currency = currency,
                    bankName = bankName,
                    last4Digits = last4
                )
            }
            
            // 3. Analytics Bar Chart
            item {
                AnalyticsBarChart()
            }
            
            // Recent Transactions Section
            item {
                Spacer(modifier = Modifier.height(Spacing.xs))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                SectionHeader(
                    title = "Recent Transactions",
                    action = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search button
                            IconButton(
                                onClick = onNavigateToTransactionsWithSearch,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search transactions",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // View All button
                            TextButton(onClick = onNavigateToTransactions) {
                                Text("View All")
                            }
                        }
                    }
                )
            }
            
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimensions.Component.minTouchTarget * 2),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.recentTransactions.isEmpty()) {
                item {
                    SavoraCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimensions.Padding.card),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "No transactions yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap the sync button below to scan your SMS and automatically detect transactions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.scanSmsMessages() },
                                modifier = Modifier.padding(top = Spacing.xs)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text("Scan SMS")
                            }
                        }
                    }
                }
            } else {
                items(
                    items = uiState.recentTransactions,
                    key = { it.id }
                ) { transaction ->
                    SimpleTransactionItem(
                        transaction = transaction,
                        convertedAmount = uiState.recentTransactionConvertedAmounts[transaction.id],
                        displayCurrency = if (uiState.isUnifiedMode) uiState.selectedCurrency else null,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
        }
        
        // Full Resync Confirmation Dialog
        if (showFullResyncDialog) {
            AlertDialog(
                onDismissRequest = { showFullResyncDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text("Full Resync")
                },
                text = {
                    Text(
                        "This will reprocess all SMS messages from scratch. " +
                        "Use this to fix issues caused by updated bank parsers.\n\n" +
                        "This may take a few seconds depending on your message history."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showFullResyncDialog = false
                            viewModel.scanSmsMessages(forceResync = true)
                        }
                    ) {
                        Text("Resync All")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showFullResyncDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // SMS Parsing Progress Dialog
        SmsParsingProgressDialog(
            isVisible = uiState.isScanning,
            workInfo = smsScanWorkInfo,
            onDismiss = { viewModel.cancelSmsScan() },
            onCancel = { viewModel.cancelSmsScan() }
        )
        
        // Breakdown Dialog
        if (uiState.showBreakdownDialog) {
            BreakdownDialog(
                currentMonthIncome = uiState.currentMonthIncome,
                currentMonthExpenses = uiState.currentMonthExpenses,
                currentMonthTotal = uiState.currentMonthTotal,
                lastMonthIncome = uiState.lastMonthIncome,
                lastMonthExpenses = uiState.lastMonthExpenses,
                lastMonthTotal = uiState.lastMonthTotal,
                currency = uiState.selectedCurrency,
                onDismiss = { viewModel.hideBreakdownDialog() }
            )
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTransactionItem(
    transaction: TransactionEntity,
    convertedAmount: BigDecimal? = null,
    displayCurrency: String? = null,
    onClick: () -> Unit = {}
) {
    val amountColor = when (transaction.transactionType) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary // Purple or Green based on mockup, let's use primary for positive or primary green
        else -> MaterialTheme.colorScheme.onBackground
    }
    
    val displayAmountColor = if (transaction.transactionType == TransactionType.INCOME) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground

    val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
    val dateTimeText = transaction.dateTime.format(dateTimeFormatter)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
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
                BrandIcon(
                    merchantName = transaction.merchantName,
                    size = 28.dp,
                    showBackground = false
                )
            }
            
            Column {
                Text(
                    text = transaction.merchantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateTimeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val amountStr = if (convertedAmount != null && displayCurrency != null) {
                CurrencyFormatter.formatCurrency(convertedAmount, displayCurrency)
            } else {
                transaction.formatAmount()
            }
            Text(
                text = "${if (transaction.transactionType == TransactionType.INCOME) "+" else ""}$amountStr",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = displayAmountColor
            )
        }
    }
}

@Composable
private fun MonthSummaryCard(
    monthTotal: BigDecimal,
    monthlyChange: BigDecimal,
    monthlyChangePercent: Int,
    currency: String,
    currentExpenses: BigDecimal = BigDecimal.ZERO,
    lastExpenses: BigDecimal = BigDecimal.ZERO,
    onShowBreakdown: () -> Unit = {}
) {
    val isPositive = monthTotal >= BigDecimal.ZERO
    val displayAmount = if (isPositive) {
        "+${CurrencyFormatter.formatCurrency(monthTotal, currency)}"
    } else {
        CurrencyFormatter.formatCurrency(monthTotal, currency)
    }
    val amountColor = if (isPositive) {
        if (!isSystemInDarkTheme()) income_light else income_dark
    } else {
        if (!isSystemInDarkTheme()) expense_light else expense_dark
    }
    
    val expenseChange = currentExpenses - lastExpenses
    val now = LocalDate.now()
    val lastMonth = now.minusMonths(1)
    val periodLabel = "vs ${lastMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} 1-${now.dayOfMonth}"
    
    val subtitle = when {
        // No transactions yet
        currentExpenses == BigDecimal.ZERO && lastExpenses == BigDecimal.ZERO -> {
            "No transactions yet"
        }
        // Spent more than last period
        expenseChange > BigDecimal.ZERO -> {
            "😟 Spent ${CurrencyFormatter.formatCurrency(expenseChange.abs(), currency)} more $periodLabel"
        }
        // Spent less than last period
        expenseChange < BigDecimal.ZERO -> {
            "😊 Spent ${CurrencyFormatter.formatCurrency(expenseChange.abs(), currency)} less $periodLabel"
        }
        // Saved more (higher positive balance)
        monthlyChange > BigDecimal.ZERO && monthTotal > BigDecimal.ZERO -> {
            "🎉 Saved ${CurrencyFormatter.formatCurrency(monthlyChange.abs(), currency)} more $periodLabel"
        }
        // No change
        else -> {
            "Same as last period"
        }
    }
    
    val currentMonth = now.month.name.lowercase().replaceFirstChar { it.uppercase() }

    val currencySymbol = CurrencyFormatter.getCurrencySymbol(currency)

    val titleText = "Cash Flow ($currencySymbol) • $currentMonth 1-${now.dayOfMonth}"
    
    // Background color: Custom Teal (from mockup)
    // Dark mode gets a slightly darker version
    val backgroundColor = if (!isSystemInDarkTheme()) {
        Color(0xFF568C79) // Light Teal
    } else {
        Color(0xFF3A6B59) // Dark Teal
    }
    
    DashboardSummaryCard(
        title = titleText,
        amount = displayAmount,
        subtitle = subtitle,
        containerColor = backgroundColor,
        icon = {
            Icon(
                imageVector = Icons.Default.Wallet,
                contentDescription = null,
                tint = backgroundColor,
                modifier = Modifier.size(24.dp)
            )
        },
        onClick = onShowBreakdown
    )
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit = {}
) {
    val amountColor = when (transaction.transactionType) {
        TransactionType.INCOME -> if (!isSystemInDarkTheme()) income_light else income_dark
        TransactionType.EXPENSE -> if (!isSystemInDarkTheme()) expense_light else expense_dark
        TransactionType.CREDIT -> if (!isSystemInDarkTheme()) credit_light else credit_dark
        TransactionType.TRANSFER -> if (!isSystemInDarkTheme()) transfer_light else transfer_dark
        TransactionType.INVESTMENT -> if (!isSystemInDarkTheme()) investment_light else investment_dark
    }
    
    // Get subtle background color based on transaction type
    val cardBackgroundColor = when (transaction.transactionType) {
        TransactionType.CREDIT -> (if (!isSystemInDarkTheme()) credit_light else credit_dark).copy(alpha = 0.05f)
        TransactionType.TRANSFER -> (if (!isSystemInDarkTheme()) transfer_light else transfer_dark).copy(alpha = 0.05f)
        TransactionType.INVESTMENT -> (if (!isSystemInDarkTheme()) investment_light else investment_dark).copy(alpha = 0.05f)
        TransactionType.INCOME -> (if (!isSystemInDarkTheme()) income_light else income_dark).copy(alpha = 0.03f)
        else -> Color.Transparent // Default for regular expenses
    }
    
    ListItemCard(
        title = transaction.merchantName,
        subtitle = transaction.dateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a")),
        amount = transaction.formatAmount(),
        amountColor = amountColor,
        onClick = onClick,
        leadingContent = {
            BrandIcon(
                merchantName = transaction.merchantName,
                size = 40.dp,
                showBackground = true
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Show icon for transaction types
                when (transaction.transactionType) {
                    TransactionType.CREDIT -> Icon(
                        Icons.Default.CreditCard,
                        contentDescription = "Credit Card",
                        modifier = Modifier.size(Dimensions.Icon.small),
                        tint = if (!isSystemInDarkTheme()) credit_light else credit_dark
                    )
                    TransactionType.TRANSFER -> Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = "Transfer",
                        modifier = Modifier.size(Dimensions.Icon.small),
                        tint = if (!isSystemInDarkTheme()) transfer_light else transfer_dark
                    )
                    TransactionType.INVESTMENT -> Icon(
                        Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Investment",
                        modifier = Modifier.size(Dimensions.Icon.small),
                        tint = if (!isSystemInDarkTheme()) investment_light else investment_dark
                    )
                    TransactionType.INCOME -> Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Income",
                        modifier = Modifier.size(Dimensions.Icon.small),
                        tint = if (!isSystemInDarkTheme()) income_light else income_dark
                    )
                    TransactionType.EXPENSE -> Icon(
                        Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = "Expense",
                        modifier = Modifier.size(Dimensions.Icon.small),
                        tint = if (!isSystemInDarkTheme()) expense_light else expense_dark
                    )
                }
                
                // Always show amount
                Text(
                    text = transaction.formatAmount(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreakdownDialog(
    currentMonthIncome: BigDecimal,
    currentMonthExpenses: BigDecimal,
    currentMonthTotal: BigDecimal,
    lastMonthIncome: BigDecimal,
    lastMonthExpenses: BigDecimal,
    lastMonthTotal: BigDecimal,
    currency: String = "INR",
    onDismiss: () -> Unit
) {
    val now = LocalDate.now()
    val currentPeriod = "${now.month.name.lowercase().replaceFirstChar { it.uppercase() }} 1-${now.dayOfMonth}"
    val lastMonth = now.minusMonths(1)
    val lastPeriod = "${lastMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} 1-${now.dayOfMonth}"
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md), // Reduced horizontal padding for wider modal
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.card),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Title
                Text(
                    text = "Calculation Breakdown",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Current Period Section
                Text(
                    text = currentPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                BreakdownRow(
                    label = "Income",
                    amount = currentMonthIncome,
                    isIncome = true,
                    currency = currency
                )

                BreakdownRow(
                    label = "Expenses",
                    amount = currentMonthExpenses,
                    isIncome = false,
                    currency = currency
                )

                HorizontalDivider()

                BreakdownRow(
                    label = "Cash Flow",
                    amount = currentMonthTotal,
                    isIncome = currentMonthTotal >= BigDecimal.ZERO,
                    isBold = true,
                    currency = currency
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Last Period Section
                Text(
                    text = lastPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                BreakdownRow(
                    label = "Income",
                    amount = lastMonthIncome,
                    isIncome = true,
                    currency = currency
                )

                BreakdownRow(
                    label = "Expenses",
                    amount = lastMonthExpenses,
                    isIncome = false,
                    currency = currency
                )

                HorizontalDivider()

                BreakdownRow(
                    label = "Cash Flow",
                    amount = lastMonthTotal,
                    isIncome = lastMonthTotal >= BigDecimal.ZERO,
                    isBold = true,
                    currency = currency
                )
                
                // Formula explanation
                Spacer(modifier = Modifier.height(Spacing.sm))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Formula: Income - Expenses = Cash Flow\n" +
                               "Green (+) = Savings | Red (-) = Overspending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(Spacing.sm),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: BigDecimal,
    isIncome: Boolean,
    isBold: Boolean = false,
    currency: String = "INR"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.formatCurrency(amount.abs(), currency)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isIncome) {
                if (!isSystemInDarkTheme()) income_light else income_dark
            } else {
                if (!isSystemInDarkTheme()) expense_light else expense_dark
            }
        )
    }
}

@Composable
private fun UpcomingSubscriptionsCard(
    subscriptions: List<SubscriptionEntity>,
    totalAmount: BigDecimal,
    currency: String = "INR",
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
                Column {
                    Text(
                        text = "${subscriptions.size} active subscriptions",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Monthly total: ${CurrencyFormatter.formatCurrency(totalAmount, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = Dimensions.Alpha.subtitle)
                    )
                }
            }
            Text(
                text = "View",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionSummaryCards(
    uiState: HomeUiState,
    onCurrencySelected: (String) -> Unit = {},
    onTypeClick: (String?) -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 4 })

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Enhanced Currency Selector (if multiple currencies available)
        if (uiState.availableCurrencies.size > 1 && !uiState.isUnifiedMode) {
            EnhancedCurrencySelector(
                selectedCurrency = uiState.selectedCurrency,
                availableCurrencies = uiState.availableCurrencies,
                onCurrencySelected = onCurrencySelected,
                modifier = Modifier.fillMaxWidth()
            )
        }
  
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = Spacing.md
        ) { page ->
            when (page) {
                0 -> {
                    // Net Balance Card (existing implementation)
                    MonthSummaryCard(
                        monthTotal = uiState.currentMonthTotal,
                        monthlyChange = uiState.monthlyChange,
                        monthlyChangePercent = uiState.monthlyChangePercent,
                        currency = uiState.selectedCurrency,
                        currentExpenses = uiState.currentMonthExpenses,
                        lastExpenses = uiState.lastMonthExpenses,
                        onShowBreakdown = { onTypeClick(null) }
                    )
                }
                1 -> {
                    // Credit Card Summary
                    TransactionTypeCard(
                        title = "Credit Card",
                        icon = Icons.Default.CreditCard,
                        amount = uiState.currentMonthCreditCard,
                        color = if (!isSystemInDarkTheme()) credit_light else credit_dark,
                        emoji = "💳",
                        currency = uiState.selectedCurrency,
                        onClick = { onTypeClick("CREDIT") }
                    )
                }
                2 -> {
                    // Transfer Summary
                    TransactionTypeCard(
                        title = "Transfers",
                        icon = Icons.Default.SwapHoriz,
                        amount = uiState.currentMonthTransfer,
                        color = if (!isSystemInDarkTheme()) transfer_light else transfer_dark,
                        emoji = "↔️",
                        currency = uiState.selectedCurrency,
                        onClick = { onTypeClick("TRANSFER") }
                    )
                }
                3 -> {
                    // Investment Summary
                    TransactionTypeCard(
                        title = "Investments",
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        amount = uiState.currentMonthInvestment,
                        color = if (!isSystemInDarkTheme()) investment_light else investment_dark,
                        emoji = "📈",
                        currency = uiState.selectedCurrency,
                        onClick = { onTypeClick("INVESTMENT") }
                    )
                }
            }
        }
        
        // Page Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val isSelected = pagerState.currentPage == index
                val color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(
                            color = color,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    amount: BigDecimal,
    color: Color,
    emoji: String,
    currency: String,
    onClick: () -> Unit = {}
) {
    val currentMonth = LocalDate.now().month.name.lowercase().replaceFirstChar { it.uppercase() }
    val now = LocalDate.now()

    val subtitle = when {
        amount > BigDecimal.ZERO -> {
            when (title) {
                "Credit Card" -> "Spent on credit this month"
                "Transfers" -> "Moved between accounts"
                "Investments" -> "Invested this month"
                else -> "Total this month"
            }
        }
        else -> {
            when (title) {
                "Credit Card" -> "No credit card spending"
                "Transfers" -> "No transfers this month"
                "Investments" -> "No investments this month"
                else -> "No transactions"
            }
        }
    }
    
    // Default color palette fallback to native theme if `color` is too generic
    // However, mapping `color` parameter back to background style.
    val backgroundColor = when (title) {
        "Credit Card" -> if (!isSystemInDarkTheme()) Color(0xFFE57373) else Color(0xFFC62828) // Redish
        "Transfers" -> if (!isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF1565C0) // Blueish
        "Investments" -> if (!isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32) // Greenish
        else -> color
    }

    DashboardSummaryCard(
        title = "$emoji $title • $currentMonth",
        amount = CurrencyFormatter.formatCurrency(amount, currency),
        subtitle = subtitle,
        containerColor = backgroundColor,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = backgroundColor,
                modifier = Modifier.size(24.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun EnhancedCurrencySelector(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Compact segmented button style
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableCurrencies.forEach { currency ->
                val isSelected = selectedCurrency == currency
                val symbol = CurrencyFormatter.getCurrencySymbol(currency)

                Surface(
                    onClick = { onCurrencySelected(currency) },
                    modifier = Modifier
                        .weight(1f)
                        .animateContentSize(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (symbol != currency) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetSummaryHomeCard(
    summary: BudgetOverallSummary,
    currency: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val limitRemaining = summary.totalLimitBudget - summary.totalLimitSpent
    val pctUsed = if (summary.totalLimitBudget > BigDecimal.ZERO) {
        (summary.totalLimitSpent.toFloat() / summary.totalLimitBudget.toFloat() * 100f)
    } else 0f

    val progressColor = when {
        pctUsed < 50f -> if (isDark) budget_safe_dark else budget_safe_light
        pctUsed < 80f -> if (isDark) budget_warning_dark else budget_warning_light
        else -> if (isDark) budget_danger_dark else budget_danger_light
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (summary.daysRemaining > 0 && summary.dailyAllowance > BigDecimal.ZERO) {
                    Text(
                        text = "${CurrencyFormatter.formatCurrency(summary.dailyAllowance, currency)}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (summary.totalLimitBudget > BigDecimal.ZERO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = CurrencyFormatter.formatCurrency(summary.totalLimitSpent, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Text(
                        text = "/ ${CurrencyFormatter.formatCurrency(summary.totalLimitBudget, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                LinearProgressIndicator(
                    progress = { (pctUsed / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )

                Text(
                    text = if (limitRemaining >= BigDecimal.ZERO) {
                        "${CurrencyFormatter.formatCurrency(limitRemaining, currency)} remaining"
                    } else {
                        "${CurrencyFormatter.formatCurrency(limitRemaining.abs(), currency)} over budget"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (summary.totalIncome > BigDecimal.ZERO) {
                val savingsColor = if (summary.netSavings >= BigDecimal.ZERO) {
                    if (isDark) budget_safe_dark else budget_safe_light
                } else {
                    if (isDark) budget_danger_dark else budget_danger_light
                }

                Text(
                    text = "${if (summary.netSavings >= BigDecimal.ZERO) "Saved" else "Overspent"} ${CurrencyFormatter.formatCurrency(summary.netSavings.abs(), currency)} (${String.format("%.0f", kotlin.math.abs(summary.savingsRate))}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = savingsColor
                )
            }
        }
    }
}

@Composable
fun HomeTopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Pic
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
            text = "Home",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Box(
            contentAlignment = Alignment.TopEnd
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = "Notifications",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
            // Red dot
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
fun MainAtmCard(
    expenseThisMonth: BigDecimal,
    currency: String,
    bankName: String,
    last4Digits: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E3142)) // Dark Color
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Expense of this month",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = CurrencyFormatter.formatCurrency(expenseThisMonth, currency),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("**** **** ****", color = Color.White.copy(alpha = 0.7f))
                        Text(last4Digits, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Text(bankName, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AnalyticsBarChart() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Year Dropdown
            Surface(
                color = MaterialTheme.colorScheme.secondary, // Orange
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Year - 2022", color = Color.White, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        
        // Bar Chart
        Row(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul")
            val amounts = listOf("$1,494", "$1,664", "$1,544", "$2,972", "$2,484", "$2,364", "$3,894")
            val heights = listOf(0.4f, 0.5f, 0.45f, 0.8f, 0.6f, 0.55f, 0.9f)
            
            months.forEachIndexed { index, month ->
                val isActive = month == "Apr"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Text(
                        text = amounts[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha=0.5f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight(heights[index])
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha=0.3f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = month,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) MaterialTheme.colorScheme.onBackground else Color.Gray
                    )
                }
            }
        }
    }
}
