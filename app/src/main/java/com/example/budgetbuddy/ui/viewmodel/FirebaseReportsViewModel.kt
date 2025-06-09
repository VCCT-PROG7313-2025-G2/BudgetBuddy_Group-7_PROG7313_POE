package com.example.budgetbuddy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.firebase.repository.FirebaseBudgetRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseExpenseRepository
import com.example.budgetbuddy.util.FirebaseSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class FirebaseReportsViewModel @Inject constructor(
    private val expenseRepository: FirebaseExpenseRepository,
    private val budgetRepository: FirebaseBudgetRepository,
    private val sessionManager: FirebaseSessionManager
) : ViewModel() {

    private val _monthlySummary = MutableStateFlow(MonthlySummary())
    val monthlySummary: StateFlow<MonthlySummary> = _monthlySummary.asStateFlow()

    private val _categorySpending = MutableStateFlow<List<CategorySpending>>(emptyList())
    val categorySpending: StateFlow<List<CategorySpending>> = _categorySpending.asStateFlow()

    private val _spendingTrend = MutableStateFlow(SpendingTrend())
    val spendingTrend: StateFlow<SpendingTrend> = _spendingTrend.asStateFlow()

    private val _weeklySpending = MutableStateFlow<List<WeeklySpending>>(emptyList())
    val weeklySpending: StateFlow<List<WeeklySpending>> = _weeklySpending.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Time period analysis
    private val _timePeriodAnalysis = MutableStateFlow(TimePeriodAnalysis())
    val timePeriodAnalysis: StateFlow<TimePeriodAnalysis> = _timePeriodAnalysis.asStateFlow()

    private val _periodSpendingData = MutableStateFlow<List<DailySpending>>(emptyList())
    val periodSpendingData: StateFlow<List<DailySpending>> = _periodSpendingData.asStateFlow()

    /**
     * Load reports data for a specific month
     */
    fun loadReportsData(monthYear: String) {
        if (!sessionManager.isLoggedIn()) return
        
        val userId = sessionManager.getUserId()

        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("ReportsViewModel", "=== Loading Reports Data ===")
                Log.d("ReportsViewModel", "Month/Year: $monthYear")
                Log.d("ReportsViewModel", "User ID: $userId")
                Log.d("ReportsViewModel", "Is logged in: ${sessionManager.isLoggedIn()}")

                // Load current month data
                            loadMonthlySummary(userId, monthYear)
            loadCategorySpending(userId, monthYear)
            loadSpendingTrend(userId, monthYear)
            loadWeeklySpending(userId, monthYear)

            } catch (e: Exception) {
                Log.e("ReportsViewModel", "Error loading reports data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadMonthlySummary(userId: String, monthYear: String) {
        try {
            val startDate = getStartOfMonth(monthYear)
            val endDate = getEndOfMonth(monthYear)

            // Get current month spending
            val currentMonthSpending = expenseRepository.getTotalSpendingBetween(userId, startDate, endDate)

            // Get previous month spending for comparison
            val previousMonth = getPreviousMonth(monthYear)
            val previousStartDate = getStartOfMonth(previousMonth)
            val previousEndDate = getEndOfMonth(previousMonth)
            val previousMonthSpending = expenseRepository.getTotalSpendingBetween(userId, previousStartDate, previousEndDate)

            // Calculate percentage change
            val percentageChange = if (previousMonthSpending > BigDecimal.ZERO) {
                ((currentMonthSpending - previousMonthSpending) / previousMonthSpending * BigDecimal(100))
                    .setScale(1, RoundingMode.HALF_UP).toDouble()
            } else {
                if (currentMonthSpending > BigDecimal.ZERO) 100.0 else 0.0
            }

            // Get budget information
            val budget = budgetRepository.getBudgetForMonthDirect(userId, monthYear)
            val budgetAmount = budget?.getTotalAmountAsBigDecimal() ?: BigDecimal.ZERO

            val summary = MonthlySummary(
                totalSpent = currentMonthSpending.toDouble(),
                budgetAmount = budgetAmount.toDouble(),
                percentageChange = percentageChange,
                remainingBudget = (budgetAmount - currentMonthSpending).toDouble()
            )

            _monthlySummary.value = summary
            Log.d("ReportsViewModel", "Monthly summary: $summary")

        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Error loading monthly summary", e)
        }
    }

    private suspend fun loadCategorySpending(userId: String, monthYear: String) {
        try {
            val startDate = getStartOfMonth(monthYear)
            val endDate = getEndOfMonth(monthYear)

            Log.d("ReportsViewModel", "=== Loading category spending ===")
            Log.d("ReportsViewModel", "User ID: $userId")
            Log.d("ReportsViewModel", "Month/Year: $monthYear")
            Log.d("ReportsViewModel", "Start date: $startDate")
            Log.d("ReportsViewModel", "End date: $endDate")

            // Get all expenses for the month
            val expenses = expenseRepository.getExpensesBetweenDates(userId, startDate, endDate)
            Log.d("ReportsViewModel", "Found ${expenses.size} expenses for the month")
            
            expenses.forEachIndexed { index, expense ->
                Log.d("ReportsViewModel", "Expense $index: ${expense.categoryName}, Amount: ${expense.amount}, Date: ${expense.date.toDate()}")
            }

            // Group by category and calculate totals
            val categoryTotals = expenses.groupBy { it.categoryName }
                .mapValues { (_, categoryExpenses) ->
                    categoryExpenses.sumOf { it.getAmountAsBigDecimal() }
                }

            val totalSpent = categoryTotals.values.sumOf { it }

            // Convert to CategorySpending objects with percentages
            val categorySpendingList = categoryTotals.map { (categoryName, amount) ->
                val percentage = if (totalSpent > BigDecimal.ZERO) {
                    (amount / totalSpent * BigDecimal(100)).setScale(1, RoundingMode.HALF_UP).toDouble()
                } else {
                    0.0
                }

                CategorySpending(
                    categoryName = categoryName,
                    amount = amount.toDouble(),
                    percentage = percentage
                )
            }.sortedByDescending { it.amount }

            _categorySpending.value = categorySpendingList
            Log.d("ReportsViewModel", "Category spending: ${categorySpendingList.size} categories")

        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Error loading category spending", e)
        }
    }

    private suspend fun loadSpendingTrend(userId: String, monthYear: String) {
        try {
            val currentMonth = getStartOfMonth(monthYear)
            val previousMonth = getStartOfMonth(getPreviousMonth(monthYear))

            val currentMonthSpending = expenseRepository.getTotalSpendingBetween(
                userId, 
                getStartOfMonth(monthYear), 
                getEndOfMonth(monthYear)
            )

            val previousMonthSpending = expenseRepository.getTotalSpendingBetween(
                userId, 
                getStartOfMonth(getPreviousMonth(monthYear)), 
                getEndOfMonth(getPreviousMonth(monthYear))
            )

            val trend = SpendingTrend(
                isIncreasing = currentMonthSpending > previousMonthSpending,
                isDecreasing = currentMonthSpending < previousMonthSpending,
                changeAmount = (currentMonthSpending - previousMonthSpending).toDouble()
            )

            _spendingTrend.value = trend
            Log.d("ReportsViewModel", "Spending trend: $trend")

        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Error loading spending trend", e)
        }
    }

    private suspend fun loadWeeklySpending(userId: String, monthYear: String) {
        try {
            val startDate = getStartOfMonth(monthYear)
            val endDate = getEndOfMonth(monthYear)

            Log.d("ReportsViewModel", "=== Loading weekly spending ===")
            
            // Get all expenses for the month
            val expenses = expenseRepository.getExpensesBetweenDates(userId, startDate, endDate)
            
            // Group expenses by week
            val weeklyTotals = mutableMapOf<Int, Double>()
            val calendar = Calendar.getInstance()
            
            expenses.forEach { expense ->
                calendar.time = expense.getDateAsDate()
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                // Calculate which week of the month (1-4)
                val weekOfMonth = ((dayOfMonth - 1) / 7) + 1
                weeklyTotals[weekOfMonth] = (weeklyTotals[weekOfMonth] ?: 0.0) + expense.getAmountAsBigDecimal().toDouble()
            }
            
            // Create weekly spending list for all 4 weeks
            val weeklySpendingList = (1..4).map { week ->
                val amount = weeklyTotals[week] ?: 0.0
                
                WeeklySpending(
                    week = week,
                    weekName = "Week $week",
                    amount = amount
                )
            }
            
            _weeklySpending.value = weeklySpendingList
            Log.d("ReportsViewModel", "Weekly spending: ${weeklySpendingList.size} weeks")

        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Error loading weekly spending", e)
        }
    }

    /**
     * Export report functionality
     */
    fun exportReport() {
        viewModelScope.launch {
            try {
                Log.d("ReportsViewModel", "Exporting report...")
                // In a real implementation, you would:
                // 1. Generate PDF report
                // 2. Save to device storage
                // 3. Share via email/other apps
                
                // For now, just log the action
                Log.d("ReportsViewModel", "Report export completed")
            } catch (e: Exception) {
                Log.e("ReportsViewModel", "Error exporting report", e)
            }
        }
    }

    // Helper functions
    private fun getStartOfMonth(monthYear: String): Date {
        val parts = monthYear.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1 // Calendar months are 0-based
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getEndOfMonth(monthYear: String): Date {
        val parts = monthYear.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1 // Calendar months are 0-based
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        return calendar.time
    }

    private fun getPreviousMonth(monthYear: String): String {
        val parts = monthYear.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1) // month - 1 because Calendar is 0-based
        calendar.add(Calendar.MONTH, -1)
        
        val formatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    // Data classes
    data class MonthlySummary(
        val totalSpent: Double = 0.0,
        val budgetAmount: Double = 0.0,
        val percentageChange: Double = 0.0,
        val remainingBudget: Double = 0.0
    )

    data class CategorySpending(
        val categoryName: String,
        val amount: Double,
        val percentage: Double
    )

    data class SpendingTrend(
        val isIncreasing: Boolean = false,
        val isDecreasing: Boolean = false,
        val changeAmount: Double = 0.0
    )

    data class WeeklySpending(
        val week: Int,
        val weekName: String,
        val amount: Double
    )

    data class TimePeriodAnalysis(
        val totalSpent: Double = 0.0,
        val dailyAverage: Double = 0.0,
        val trend: String = "--", // "Rising", "Falling", "Stable"
        val periodLabel: String = "",
        val startDate: Date? = null,
        val endDate: Date? = null
    )

    data class DailySpending(
        val date: Date,
        val amount: Double,
        val formattedDate: String = ""
    )

    enum class TimePeriod {
        WEEK, MONTH, QUARTER, YEAR, CUSTOM
    }

    /**
     * Load spending analysis data for a specific time period and category
     */
    fun loadTimePeriodAnalysis(period: TimePeriod, startDate: Date? = null, endDate: Date? = null, categoryFilter: String? = null) {
        if (!sessionManager.isLoggedIn()) return
        
        val userId = sessionManager.getUserId()

        viewModelScope.launch {
            try {
                Log.d("ReportsViewModel", "=== Loading Time Period Analysis ===")
                Log.d("ReportsViewModel", "Period: $period")
                
                val (analysisStartDate, analysisEndDate, periodLabel) = when (period) {
                    TimePeriod.WEEK -> {
                        val end = Calendar.getInstance().time
                        val start = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -7)
                        }.time
                        Triple(start, end, "Last 7 Days")
                    }
                    TimePeriod.MONTH -> {
                        val end = Calendar.getInstance().time
                        val start = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -30)
                        }.time
                        Triple(start, end, "Last 30 Days")
                    }
                    TimePeriod.QUARTER -> {
                        val end = Calendar.getInstance().time
                        val start = Calendar.getInstance().apply {
                            add(Calendar.MONTH, -3)
                        }.time
                        Triple(start, end, "Last 3 Months")
                    }
                    TimePeriod.YEAR -> {
                        val end = Calendar.getInstance().time
                        val start = Calendar.getInstance().apply {
                            add(Calendar.YEAR, -1)
                        }.time
                        Triple(start, end, "Last 12 Months")
                    }
                    TimePeriod.CUSTOM -> {
                        if (startDate != null && endDate != null) {
                            val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
                            val label = "${formatter.format(startDate)} - ${formatter.format(endDate)}"
                            Triple(startDate, endDate, label)
                        } else {
                            return@launch
                        }
                    }
                }

                Log.d("ReportsViewModel", "Start: $analysisStartDate, End: $analysisEndDate")

                // Get expenses for the period
                val allExpenses = expenseRepository.getExpensesBetweenDates(userId, analysisStartDate, analysisEndDate)
                
                // Filter by category if specified
                val expenses = if (categoryFilter != null && categoryFilter != "All Categories") {
                    allExpenses.filter { it.categoryName == categoryFilter }
                } else {
                    allExpenses
                }
                
                Log.d("ReportsViewModel", "Category filter: $categoryFilter")
                Log.d("ReportsViewModel", "Total expenses: ${allExpenses.size}, Filtered: ${expenses.size}")
                
                // Calculate total spent
                val totalSpent = expenses.sumOf { it.getAmountAsBigDecimal() }
                
                // Calculate daily average
                val daysDiff = ((analysisEndDate.time - analysisStartDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1
                val dailyAverage = if (daysDiff > 0) totalSpent.toDouble() / daysDiff else 0.0
                
                // Calculate trend (comparing first half vs second half of period)
                val midPoint = Date((analysisStartDate.time + analysisEndDate.time) / 2)
                val firstHalfExpenses = expenses.filter { it.getDateAsDate().before(midPoint) }
                val secondHalfExpenses = expenses.filter { it.getDateAsDate().after(midPoint) }
                
                val firstHalfTotal = firstHalfExpenses.sumOf { it.getAmountAsBigDecimal() }
                val secondHalfTotal = secondHalfExpenses.sumOf { it.getAmountAsBigDecimal() }
                
                val trend = when {
                    secondHalfTotal > firstHalfTotal * BigDecimal("1.1") -> "↗ Rising"
                    secondHalfTotal < firstHalfTotal * BigDecimal("0.9") -> "↘ Falling"
                    else -> "→ Stable"
                }

                // Update analysis state
                _timePeriodAnalysis.value = TimePeriodAnalysis(
                    totalSpent = totalSpent.toDouble(),
                    dailyAverage = dailyAverage,
                    trend = trend,
                    periodLabel = periodLabel,
                    startDate = analysisStartDate,
                    endDate = analysisEndDate
                )

                // Group expenses by day for the chart
                val dailySpendingMap = mutableMapOf<String, Double>()
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                
                // Initialize all days in the period with 0
                calendar.time = analysisStartDate
                while (!calendar.time.after(analysisEndDate)) {
                    val dateKey = dateFormatter.format(calendar.time)
                    dailySpendingMap[dateKey] = 0.0
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                // Add actual spending data
                expenses.forEach { expense ->
                    val dateKey = dateFormatter.format(expense.getDateAsDate())
                    dailySpendingMap[dateKey] = (dailySpendingMap[dateKey] ?: 0.0) + expense.getAmountAsBigDecimal().toDouble()
                }

                // Convert to DailySpending list
                val dailySpendingList = dailySpendingMap.entries.sortedBy { it.key }.map { (dateStr, amount) ->
                    val date = dateFormatter.parse(dateStr) ?: Date()
                    val displayFormatter = when (period) {
                        TimePeriod.WEEK -> SimpleDateFormat("EEE", Locale.getDefault())
                        TimePeriod.MONTH -> SimpleDateFormat("MMM d", Locale.getDefault())
                        TimePeriod.QUARTER, TimePeriod.YEAR -> SimpleDateFormat("MMM", Locale.getDefault())
                        TimePeriod.CUSTOM -> SimpleDateFormat("MMM d", Locale.getDefault())
                    }
                    
                    DailySpending(
                        date = date,
                        amount = amount,
                        formattedDate = displayFormatter.format(date)
                    )
                }

                _periodSpendingData.value = dailySpendingList
                
                Log.d("ReportsViewModel", "Time period analysis loaded: Total: $totalSpent, Daily Avg: $dailyAverage, Trend: $trend")

            } catch (e: Exception) {
                Log.e("ReportsViewModel", "Error loading time period analysis", e)
            }
        }
    }

    /**
     * Get available expense categories for filtering
     */
    suspend fun getAvailableCategories(): List<String> {
        if (!sessionManager.isLoggedIn()) return emptyList()
        
        val userId = sessionManager.getUserId()
        return try {
            // Get categories used in the last year to have a comprehensive list
            val endDate = Date()
            val startDate = Calendar.getInstance().apply {
                time = endDate
                add(Calendar.YEAR, -1)
            }.time
            
            val categories = expenseRepository.getUsedCategoriesInPeriod(userId, startDate, endDate)
            Log.d("ReportsViewModel", "Available categories: $categories")
            categories.sorted()
        } catch (e: Exception) {
            Log.e("ReportsViewModel", "Error getting categories", e)
            emptyList()
        }
    }
} 