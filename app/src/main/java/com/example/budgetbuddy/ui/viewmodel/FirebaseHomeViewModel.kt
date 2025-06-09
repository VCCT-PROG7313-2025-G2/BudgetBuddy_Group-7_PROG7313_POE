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
import com.example.budgetbuddy.util.CurrencyConverter

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
    private val sessionManager: FirebaseSessionManager,
    private val currencyConverter: CurrencyConverter
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
            Log.e("FirebaseHomeViewModel", "Oops! No user is logged in, can't load their data")
            _uiState.value = FirebaseHomeUiState(isLoading = false, error = "Please log in.")
            return
        }

        Log.d("FirebaseHomeViewModel", "Loading home screen for user: $userId")
        
        viewModelScope.launch {
            try {
                // Let's figure out what month we're dealing with and set up our date ranges
                val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthYear = monthYearFormat.format(Date())
                val startOfMonthDate = getStartOfMonth()
                val endOfMonthDate = getEndOfMonth()
                val weekStartDate = getStartOfWeek()
                val todayEndDate = getEndOfDay()

                Log.d("FirebaseHomeViewModel", "Working with month: $currentMonthYear")

                // Set up our data streams - these will update automatically when things change
                val userFlow = authRepository.getUserProfileFlow(userId)
                val budgetFlow = budgetRepository.getBudgetForMonth(userId, currentMonthYear)
                val monthlySpendingFlow = getMonthlySpendingFlow(userId, startOfMonthDate, endOfMonthDate)
                val categorySpendingFlow = expenseRepository.getSpendingByCategoryBetweenFlow(userId, startOfMonthDate, endOfMonthDate)
                
                Log.d("FirebaseHomeViewModel", "Set up data streams, now watching for changes...")

                // Combine all our data sources and react to any changes
                combine(
                    userFlow,
                    budgetFlow,
                    monthlySpendingFlow,
                    categorySpendingFlow
                ) { user, budget, totalSpent, categorySpending ->
                    Log.d("FirebaseHomeViewModel", "Got fresh data! Processing for home screen...")
                    
                    // Create a nice greeting - just use their first name to keep it friendly
                    val greeting = user?.name?.substringBefore(" ")?.let { "Hi, $it" } ?: "Welcome"
                    val budgetTotal = budget?.getTotalAmountAsBigDecimal() ?: BigDecimal.ZERO
                    
                    // Calculate how much of their budget they've used (as a percentage)
                    val budgetProgress = if (budgetTotal > BigDecimal.ZERO) {
                        (totalSpent.divide(budgetTotal, 2, RoundingMode.HALF_UP) * BigDecimal(100)).toInt().coerceIn(0, 100)
                    } else 0
                    
                    Log.d("FirebaseHomeViewModel", "Budget status: R$totalSpent / R$budgetTotal (${budgetProgress}%)")
                    
                    // Build the category breakdown for display
                    val categoryUiList = generateCategoryUiList(budget, categorySpending)
                    
                    // Now for the fun part - let's build that spending chart!
                    val sevenDaysAgo = getSevenDaysAgo()
                    val today = getEndOfDay()
                    val weeklyExpenses = expenseRepository.getExpensesBetween(userId, sevenDaysAgo, today)
                    val dailySpendingData = try {
                        val expenses = weeklyExpenses.first() // Get the current list
                        Log.d("FirebaseHomeViewModel", "Building chart with ${expenses.size} expenses from the past week")
                        generateDailySpendingChartData(expenses)
                    } catch (e: Exception) {
                        Log.e("FirebaseHomeViewModel", "Couldn't build the spending chart", e)
                        null
                    }
                    
                    val positionText = "Tap to view rewards"  // Simple placeholder for now

                    // Package everything up for the UI
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
                .debounce(300) // Don't update the UI too frantically - wait for things to settle
                .collect { newState ->
                    Log.d("FirebaseHomeViewModel", "Updating home screen UI with fresh data")
                    _uiState.value = newState
                }

            } catch (e: Exception) {
                Log.e("FirebaseHomeViewModel", "Something went wrong loading home data", e)
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
     * This creates those little progress bars you see for each spending category.
     */
    private suspend fun generateCategoryUiList(
        budget: FirebaseBudget?,
        categorySpending: Map<String, BigDecimal>
    ): List<HomeCategoryItemUiState> {
        Log.d("FirebaseHomeViewModel", "Building category display list...")
        
        if (budget == null) {
            Log.d("FirebaseHomeViewModel", "No budget set yet, showing default categories")
            // If no budget exists, show default categories with 0 spending
            return getDefaultCategoryNames().map { categoryName ->
                val spent = categorySpending[categoryName] ?: BigDecimal.ZERO
                            Log.d("FirebaseHomeViewModel", "Default category $categoryName: ${currencyConverter.formatAmount(spent)} spent (no budget)")
            HomeCategoryItemUiState(
                name = categoryName,
                progress = 0, // No budget set
                percentageText = "No budget set",
                iconResId = getCategoryIconRes(categoryName)
            )
            }
        }

        // Get all the categories that actually have money allocated to them
        val allCategoryBudgets = budgetRepository.getCategoryBudgetsForBudgetDirect(budget.id)
        val categoryBudgets = allCategoryBudgets.filter { it.allocatedAmount > 0.0 }
        
        Log.d("FirebaseHomeViewModel", "Found ${categoryBudgets.size} categories with budgets allocated")
        
        return categoryBudgets.map { categoryBudget ->
            val spent = categorySpending[categoryBudget.categoryName] ?: BigDecimal.ZERO
            val allocated = categoryBudget.getAllocatedAmountAsBigDecimal()
            
            // Calculate how much of this category's budget has been used
            val progress = if (allocated > BigDecimal.ZERO) {
                (spent.divide(allocated, 2, RoundingMode.HALF_UP) * BigDecimal(100)).toInt().coerceIn(0, 100)
            } else {
                // If they've spent money but there's no budget, that's 100% over!
                if (spent > BigDecimal.ZERO) 100 else 0
            }
            
            // Create a nice display text showing spent vs allocated
            val percentageText = if (allocated > BigDecimal.ZERO) {
                "${currencyConverter.formatAmount(spent.toDouble())} / ${currencyConverter.formatAmount(allocated.toDouble())}"
            } else {
                if (spent > BigDecimal.ZERO) "${currencyConverter.formatAmount(spent.toDouble())} spent" else "No budget"
            }
            
            Log.d("FirebaseHomeViewModel", "Category ${categoryBudget.categoryName}: $percentageText (${progress}%)")
            
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
     * This creates the little bar chart you see on the home screen that shows your spending pattern.
     */
    private fun generateDailySpendingChartData(expenses: List<FirebaseExpense>): Pair<List<BarEntry>, List<String>> {
        Log.d("FirebaseHomeViewModel", "Starting to build the weekly spending chart - let's see what we've got...")
        
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        
        // We'll group all expenses by date so we can see daily totals
        val dailyTotals = mutableMapOf<String, BigDecimal>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Go through each expense and add it to the right day's total
        expenses.forEach { expense ->
            val dateKey = dateFormat.format(expense.getDateAsDate())
            val currentTotal = dailyTotals[dateKey] ?: BigDecimal.ZERO
            dailyTotals[dateKey] = currentTotal + expense.getAmountAsBigDecimal()
            Log.d("FirebaseHomeViewModel", "Added ${currencyConverter.formatAmount(expense.getAmountAsBigDecimal())} to $dateKey (running total: ${currencyConverter.formatAmount(dailyTotals[dateKey]!!)})")
        }

        // Now let's build the chart data for the past 7 days (including today)
        val calendar = Calendar.getInstance()
        Log.d("FirebaseHomeViewModel", "Building chart for the past week...")
        
        for (i in 6 downTo 0) { // Start from 6 days ago and work forward to today
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            
            val dateKey = dateFormat.format(calendar.time)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val dayLabel = dayLabels[dayOfWeek]
            val amount = dailyTotals[dateKey] ?: BigDecimal.ZERO
            
            // Each bar on the chart represents one day's total spending
            entries.add(BarEntry((6 - i).toFloat(), amount.toFloat()))
            labels.add(dayLabel)
            
            Log.d("FirebaseHomeViewModel", "Chart day ${dayLabel}: ${currencyConverter.formatAmount(amount)} ${if (amount == BigDecimal.ZERO) "(no spending)" else ""}")
        }

        Log.d("FirebaseHomeViewModel", "Chart ready! Created ${entries.size} bars showing your spending pattern")
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