package com.pennywiseai.tracker.presentation.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.ui.screens.analytics.AnalyticsViewModel
import com.pennywiseai.tracker.ui.screens.analytics.CalendarStrip
import com.pennywiseai.tracker.ui.screens.analytics.AnalyticsOverview
import com.pennywiseai.tracker.presentation.home.TransactionItem
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Total Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 1. Calendar Strip (Workable)
            item {
                CalendarStrip(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() }
                )
            }

            // 2. Spending Summary
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "You have Spend ${CurrencyFormatter.formatCurrency(uiState.totalSpending, uiState.currency)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "this month.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = selectedDate.month.name.lowercase().capitalize() + " " + selectedDate.year,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Income utilization bar (reusing logic from AnalyticsOverview if possible or just drawing it)
                    val utilization = if (uiState.totalIncome.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        (uiState.totalSpending.divide(uiState.totalIncome, 4, java.math.RoundingMode.HALF_UP).toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color(0xFFF1F1F1), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(utilization)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (utilization > 0.1f) {
                                Text(
                                    text = "${(utilization * 100).toInt()}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        if (utilization <= 0.1f) {
                            Text(
                                text = "${(utilization * 100).toInt()}%",
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 3. Analytics (Pie Chart)
            item {
                Text(
                    text = "Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
                AnalyticsOverview(uiState = uiState)
            }

            // 4. Daily Transactions
            if (uiState.dailyTransactions.isNotEmpty()) {
                item {
                    Text(
                        text = "Today's Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                items(uiState.dailyTransactions) { txWithSplits ->
                    TransactionItem(
                        transaction = txWithSplits.transaction,
                        onClick = { /* Maybe navigate to detail of transaction */ }
                    )
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions for this day", color = Color.Gray)
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Extension to capitalize first letter
private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
