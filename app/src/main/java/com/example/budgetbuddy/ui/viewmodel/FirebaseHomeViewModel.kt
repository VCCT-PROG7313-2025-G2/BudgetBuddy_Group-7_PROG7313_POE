package com.example.budgetbuddy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.R
import com.example.budgetbuddy.data.firebase.repository.FirebaseBudgetRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseExpenseRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseRewardsRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseAuthRepository
import com.example.budgetbuddy.data.firebase.model.FirebaseBudget
import com.example.budgetbuddy.data.firebase.model.FirebaseExpense
import com.example.budgetbuddy.data.firebase.model.FirebaseUser
import com.example.budgetbuddy.data.firebase.model.FirebaseRewardPoints
// Removed unused import
import com.example.budgetbuddy.util.FirebaseSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.github.mikephil.charting.data.BarEntry

/**
 * Firebase-based HomeViewModel that replaces the Room-based version.
 * Uses Firebase repositories for real-time dashboard updates.
 */

// UI State classes remain the same for compatibility
data class FirebaseHomeUiState(
    val greeting: String = "",
    val budgetTotal: BigDecimal = BigDecimal.ZERO,
    val budgetSpent: BigDecimal = BigDecimal.ZERO,
    val budgetProgress: Int = 0, // 0-100 for the progress bar
    val dailySpendingData: Pair<List<BarEntry>, List<String>>? = null,
    val budgetCategories: List<HomeCategoryItemUiState> = emptyList(),
    val leaderboardPositionText: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class HomeCategoryItemUiState(
    val name: String,
    val progress: Int, // Progress within the category limit (0-100)
    val percentageText: String, // Formatted text like "50%"
    val iconResId: Int // Resource ID for the category icon
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class FirebaseHomeViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val budgetRepository: FirebaseBudgetRepository,
    private val expenseRepository: FirebaseExpenseRepository,
    private val rewardsRepository: FirebaseRewardsRepository,
    private val sessionManager: FirebaseSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirebaseHomeUiState(isLoading = true))
    val uiState: StateFlow<FirebaseHomeUiState> = _uiState.asStateFlow()

    private fun getCurrentUserId(): String = sessionManager.getUserId()

    init {
        loadHomeScreenData()
    }

    /**
     * Loads all data needed for the home screen with real-time updates.
     */
    private fun loadHomeScreenData() {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            Log.e("FirebaseHomeViewModel", "Cannot load data, no user logged in")
            _uiState.value = FirebaseHomeUiState(isLoading = false, error = "Please log in.")
            return
        }

        viewModelScope.launch {
            try {
                // Define date ranges
                val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthYear = monthYearFormat.format(Date())
                val startOfMonthDate = getStartOfMonth()
                val endOfMonthDate = getEndOfMonth()
                val weekStartDate = getStartOfWeek()
                val todayEndDate = getEndOfDay()

                // Define flows for real-time data
                val userFlow = authRepository.getUserProfileFlow(userId)
                val budgetFlow = budgetRepository.getBudgetForMonth(userId, currentMonthYear)
                val monthlySpendingFlow = getMonthlySpendingFlow(userId, startOfMonthDate, endOfMonthDate)
                val categorySpendingFlow = expenseRepository.getSpendingByCategoryBetweenFlow(userId, startOfMonthDate, endOfMonthDate)
                // Removed for performance optimization
                // val weeklyExpensesFlow = expenseRepository.getExpensesBetween(userId, weekStartDate, todayEndDate)
                // val leaderboardFlow = rewardsRepository.getLeaderboardFlow(50)

                // Combine essential flows for reactive updates (optimized for performance)
                combine(
                    userFlow,
                    budgetFlow,
                    monthlySpendingFlow,
                    categorySpendingFlow
                ) { user, budget, totalSpent, categorySpending ->
                    // Generate UI data with the available information
                    // Skip chart data for better performance - will be loaded separately if needed
                    
                    // Process data for UI
                    val greeting = user?.name?.substringBefore(" ")?.let { "Hi, $it" } ?: "Welcome"
                    val budgetTotal = budget?.getTotalAmountAsBigDecimal() ?: BigDecimal.ZERO
                    val budgetProgress = if (budgetTotal > BigDecimal.ZERO) {
                        (totalSpent.divide(budgetTotal, 2, RoundingMode.HALF_UP) * BigDecimal(100)).toInt().coerceIn(0, 100)
                    } else 0
                    
                    val categoryUiList = generateCategoryUiList(budget, categorySpending)
                    
                    // Generate chart data for daily spending
                    val sevenDaysAgo = getSevenDaysAgo()
                    val today = getEndOfDay()
                    val weeklyExpenses = expenseRepository.getExpensesBetween(userId, sevenDaysAgo, today)
                    val dailySpendingData = try {
                        val expenses = weeklyExpenses.first() // Get current value
                        android.util.Log.d("FirebaseHomeViewModel", "=== Chart Data Generation ===")
                        android.util.Log.d("FirebaseHomeViewModel", "Date range: $sevenDaysAgo to $today")
                        android.util.Log.d("FirebaseHomeViewModel", "Found ${expenses.size} expenses for chart")
                        expenses.forEach { expense ->
                            android.util.Log.d("FirebaseHomeViewModel", "Chart expense: ${expense.categoryName} = ${expense.amount} on ${expense.date.toDate()}")
                        }
                        generateDailySpendingChartData(expenses)
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseHomeViewModel", "Error generating chart data", e)
                        null
                    }
                    
                    val positionText = "Tap to view rewards"

                    FirebaseHomeUiState(
                        greeting = greeting,
                        budgetTotal = budgetTotal,
                        budgetSpent = totalSpent,
                        budgetProgress = budgetProgress,
                        dailySpendingData = dailySpendingData,
                        budgetCategories = categoryUiList,
                        leaderboardPositionText = positionText,
                        isLoading = false,
                        error = null
                    )
                }
                .debounce(300) // Add debouncing to reduce UI update frequency
                .collect { newState ->
                    _uiState.value = newState
                }

            } catch (e: Exception) {
                Log.e("FirebaseHomeViewModel", "Error loading home data", e)
                _uiState.value = FirebaseHomeUiState(
                    isLoading = false,
                    error = "Failed to load data: ${e.message}"
                )
            }
        }
    }

    /**
     * Gets monthly spending as a Flow for real-time updates.
     */
    private fun getMonthlySpendingFlow(userId: String, startDate: Date, endDate: Date): Flow<BigDecimal> {
        return expenseRepository.getExpensesBetween(userId, startDate, endDate)
            .map { expenses ->
                expenses.sumOf { it.getAmountAsBigDecimal() }
            }
    }

    /**
     * Generates category UI list with spending vs budget comparison.
     * Shows ALL budget categories, not just those with spending.
     */
    private suspend fun generateCategoryUiList(
        budget: FirebaseBudget?,
        categorySpending: Map<String, BigDecimal>
    ): List<HomeCategoryItemUiState> {
        if (budget == null) {
            // If no budget exists, show default categories with 0 spending
            return getDefaultCategoryNames().map { categoryName ->
                val spent = categorySpending[categoryName] ?: BigDecimal.ZERO
                HomeCategoryItemUiState(
                    name = categoryName,
                    progress = 0, // No budget set
                    percentageText = "No budget set",
                    iconResId = getCategoryIconRes(categoryName)
                )
            }
        }

        // Get category budgets for this budget - only those with allocated amounts > 0
        val allCategoryBudgets = budgetRepository.getCategoryBudgetsForBudgetDirect(budget.id)
        val categoryBudgets = allCategoryBudgets.filter { it.allocatedAmount > 0.0 }
        
        android.util.Log.d("FirebaseHomeViewModel", "All categories: ${allCategoryBudgets.size}, with budgets: ${categoryBudgets.size}")
        categoryBudgets.forEach { category ->
            android.util.Log.d("FirebaseHomeViewModel", "Home category: ${category.categoryName} = ${category.allocatedAmount}")
        }
        
        return categoryBudgets.map { categoryBudget ->
            val spent = categorySpending[categoryBudget.categoryName] ?: BigDecimal.ZERO
            val allocated = categoryBudget.getAllocatedAmountAsBigDecimal()
            
            val progress = if (allocated > BigDecimal.ZERO) {
                (spent.divide(allocated, 2, RoundingMode.HALF_UP) * BigDecimal(100)).toInt().coerceIn(0, 100)
            } else {
                if (spent > BigDecimal.ZERO) 100 else 0 // If spending but no budget, show 100%
            }
            
            val percentageText = if (allocated > BigDecimal.ZERO) {
                "$${spent.toInt()} / $${allocated.toInt()}"
            } else {
                if (spent > BigDecimal.ZERO) "$${spent.toInt()} spent" else "No budget"
            }
            
            val iconResId = getCategoryIconRes(categoryBudget.categoryName)

            HomeCategoryItemUiState(
                name = categoryBudget.categoryName,
                progress = progress,
                percentageText = percentageText,
                iconResId = iconResId
            )
        }
    }

    /**
     * Gets default category names when no budget exists.
     */
    private fun getDefaultCategoryNames(): List<String> {
        return listOf(
            "Food & Dining",
            "Transportation", 
            "Shopping",
            "Entertainment",
            "Bills & Utilities",
            "Healthcare",
            "Education",
            "Travel"
        )
    }

    /**
     * Generates daily spending chart data for the past 7 days.
     */
    private fun generateDailySpendingChartData(expenses: List<FirebaseExpense>): Pair<List<BarEntry>, List<String>> {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        
        // Create a map to store daily totals: "yyyy-MM-dd" -> amount
        val dailyTotals = mutableMapOf<String, BigDecimal>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Process expenses and group by date
        expenses.forEach { expense ->
            val dateKey = dateFormat.format(expense.getDateAsDate())
            dailyTotals[dateKey] = (dailyTotals[dateKey] ?: BigDecimal.ZERO) + expense.getAmountAsBigDecimal()
            android.util.Log.d("FirebaseHomeViewModel", "Added expense: $dateKey = ${expense.getAmountAsBigDecimal()}, total now: ${dailyTotals[dateKey]}")
        }

        // Generate chart data for the past 7 days (including today)
        val calendar = Calendar.getInstance()
        
        for (i in 6 downTo 0) { // 6 days ago to today
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            
            val dateKey = dateFormat.format(calendar.time)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val dayLabel = dayLabels[dayOfWeek]
            val amount = dailyTotals[dateKey] ?: BigDecimal.ZERO
            
            entries.add(BarEntry((6 - i).toFloat(), amount.toFloat()))
            labels.add(dayLabel)
            
            android.util.Log.d("FirebaseHomeViewModel", "Chart day $i: $dateKey ($dayLabel) = $amount")
        }

        android.util.Log.d("FirebaseHomeViewModel", "Generated ${entries.size} chart entries")
        return Pair(entries, labels)
    }



    /**
     * Gets icon resource for a category.
     * Updated to handle all standardized category names consistently.
     */
    private fun getCategoryIconRes(categoryName: String): Int {
        return when (categoryName.lowercase()) {
            "food & dining", "food", "dining", "groceries" -> R.drawable.ic_category_food
            "transportation", "transport", "gas", "fuel" -> R.drawable.ic_category_transport
            "shopping", "clothes", "clothing" -> R.drawable.ic_category_shopping
            "bills & utilities", "utilities", "bills", "electricity", "water" -> R.drawable.ic_category_utilities
            "entertainment", "movies", "games" -> R.drawable.ic_category_other
            "healthcare", "health", "medical" -> R.drawable.ic_category_other
            "education", "school", "books" -> R.drawable.ic_category_other
            "travel", "vacation", "hotel" -> R.drawable.ic_category_other
            else -> R.drawable.ic_category_other
        }
    }

    /**
     * Refreshes the home screen data.
     */
    fun refreshData() {
        loadHomeScreenData()
    }

    /**
     * Helper functions for date calculations.
     */
    private fun getStartOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getEndOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    private fun getStartOfWeek(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getEndOfDay(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    private fun getSevenDaysAgo(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6) // 6 days ago plus today = 7 days total
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
} 