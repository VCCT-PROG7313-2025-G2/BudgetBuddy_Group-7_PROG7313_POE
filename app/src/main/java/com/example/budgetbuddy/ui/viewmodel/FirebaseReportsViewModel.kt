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
} 