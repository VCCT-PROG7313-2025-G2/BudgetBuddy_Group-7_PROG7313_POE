package com.example.budgetbuddy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.R // For placeholder drawables
import com.example.budgetbuddy.data.repository.BudgetRepository
import com.example.budgetbuddy.data.repository.ExpenseRepository
import com.example.budgetbuddy.data.repository.RewardsRepository // Keep for later
import com.example.budgetbuddy.data.repository.UserRepository
import com.example.budgetbuddy.data.db.entity.BudgetEntity
import com.example.budgetbuddy.data.db.entity.CategoryBudgetEntity
import com.example.budgetbuddy.data.db.entity.ExpenseEntity
import com.example.budgetbuddy.data.db.pojo.CategorySpending
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieEntry
import com.example.budgetbuddy.util.SessionManager // Add import
import com.example.budgetbuddy.util.DateUtils // Import DateUtils

// --- UI State Definitions --- 
// This data class holds all the information the HomeFragment needs to display.
// It includes greeting, budget numbers, chart data, category list, etc.
data class HomeUiState(
    val greeting: String = "",
    val budgetTotal: BigDecimal = BigDecimal.ZERO,
    val budgetSpent: BigDecimal = BigDecimal.ZERO,
    val budgetProgress: Int = 0, // 0-100 for the progress bar
    // Data for the daily spending bar chart: List of bar heights and corresponding labels (e.g., "Mon", "Tue").
    val dailySpendingData: Pair<List<BarEntry>, List<String>>? = null,
    // List of categories to show in the RecyclerView.
    val budgetCategories: List<HomeCategoryItemUiState> = emptyList(),
    // Text to display the user's rank.
    val leaderboardPositionText: String = "",
    // Flags to indicate if data is loading or if an error occurred.
    val isLoading: Boolean = true,
    val error: String? = null
)

// This data class holds the information needed for a single category item in the list.
data class HomeCategoryItemUiState(
    val name: String,
    val progress: Int, // Progress within the category limit (0-100)
    val percentageText: String, // Formatted text like "50%"
    val iconResId: Int // Resource ID for the category icon (e.g., R.drawable.ic_category_food)
)
// --- End UI State Definitions ---

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val rewardsRepository: RewardsRepository,
    private val sessionManager: SessionManager // Used to get the current user ID.
) : ViewModel() {

    // Holds the current state of the Home screen. Fragments observe this.
    // MutableStateFlow allows us to update the state.
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    // Expose the state as a non-mutable StateFlow to the fragment.
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Helper to get the ID of the currently logged-in user.
    private fun getCurrentUserId(): Long = sessionManager.getUserId()

    // This block runs when the ViewModel is first created.
    init {
        loadHomeScreenData() // Start loading the necessary data.
    }

    // Main function to load all data needed for the home screen.
    private fun loadHomeScreenData() {
        val userId = getCurrentUserId()
        // If no user is logged in, show an error state and stop.
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            Log.e("HomeViewModel", "Cannot load data, no user logged in")
            _uiState.value = HomeUiState(isLoading = false, error = "Please log in.")
            return
        }

        // Launch a coroutine to perform data loading off the main thread.
        viewModelScope.launch {
            try {
                // --- Define Flows for Data --- 
                // A Flow represents a stream of data that can change over time.
                val userFlow = userRepository.getUser(userId)
                val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthYear = monthYearFormat.format(Date())
                val startOfMonthDate = DateUtils.getStartOfMonth() // Use DateUtils
                val endOfMonthDate = DateUtils.getEndOfMonth()     // Use DateUtils
                val budgetFlow = budgetRepository.getBudgetForMonth(userId, currentMonthYear)
                val spentFlow = expenseRepository.getTotalSpendingBetween(userId, startOfMonthDate, endOfMonthDate).map { it ?: BigDecimal.ZERO }
                val categorySpendingFlow = expenseRepository.getSpendingByCategoryBetween(userId, startOfMonthDate, endOfMonthDate)
                val weekStartDate = DateUtils.getStartOfWeek() // Use DateUtils
                val todayEndDate = DateUtils.getEndOfDay()     // Use DateUtils
                val dailyExpensesFlow = expenseRepository.getExpensesBetween(userId, weekStartDate, todayEndDate)
                val relevantCategoriesFlow = budgetRepository.getRelevantCategoryNamesForPeriod(userId, startOfMonthDate, endOfMonthDate)
                val leaderboardFlow = rewardsRepository.getFullLeaderboard()

                // --- Combine Flows --- 
                // Combine the latest values from all flows. This runs whenever any of the flows emit a new value.
                combine(
                    userFlow, budgetFlow, spentFlow, categorySpendingFlow,
                    dailyExpensesFlow, leaderboardFlow, relevantCategoriesFlow
                ) { flows ->
                    // Extract the latest data from each flow.
                    val user = flows[0] as com.example.budgetbuddy.data.db.entity.UserEntity?
                    val budget = flows[1] as com.example.budgetbuddy.data.db.entity.BudgetEntity?
                    val spent = flows[2] as java.math.BigDecimal
                    val categorySpendingList = flows[3] as List<com.example.budgetbuddy.data.db.pojo.CategorySpending>
                    val weekExpenses = flows[4] as List<com.example.budgetbuddy.data.db.entity.ExpenseEntity>
                    val leaderboard = flows[5] as List<com.example.budgetbuddy.model.UserWithPoints>
                    val relevantCategoryNames = flows[6] as List<String>

                    // --- Process Data --- 
                    // Prepare the data for the UI State object.
                    val greeting = user?.name?.substringBefore(" ")?.let { "Hi, $it" } ?: "Welcome"
                    val budgetTotal = budget?.totalAmount ?: BigDecimal.ZERO
                    val budgetProgress = if (budgetTotal > BigDecimal.ZERO) {
                        (spent.divide(budgetTotal, 2, RoundingMode.HALF_UP) * BigDecimal(100)).toInt().coerceIn(0, 100)
                    } else 0
                    val categoryUiList = generateDynamicCategoryUiList(budget?.budgetId, categorySpendingList, relevantCategoryNames)
                    val dailySpendingData = generateDailySpendingChartData(weekExpenses)
                    val userPosition = leaderboard.indexOfFirst { it.user.userId == userId }
                    val positionText = if (userPosition != -1) {
                        "Rank #${userPosition + 1} on Leaderboard"
                    } else {
                        "Not Ranked Yet"
                    }

                    // --- Create New UI State --- 
                    HomeUiState(
                        greeting = greeting,
                        budgetTotal = budgetTotal,
                        budgetSpent = spent,
                        budgetProgress = budgetProgress,
                        dailySpendingData = dailySpendingData,
                        budgetCategories = categoryUiList,
                        leaderboardPositionText = positionText,
                        isLoading = false, // Data is loaded
                        error = null // Clear any previous error
                    )
                }.catch { e -> // Handle errors during the combine/processing phase.
                    Log.e("HomeViewModel", "Error loading home screen data", e)
                    _uiState.value = HomeUiState(isLoading = false, error = "Failed to load data")
                }.collect { newState -> // Collect the results from the combine operation.
                    _uiState.value = newState // Update the UI state with the newly processed data.
                }

            } catch (e: Exception) { // Catch any other unexpected errors during setup.
                 Log.e("HomeViewModel", "Exception in loadHomeScreenData", e)
                _uiState.value = HomeUiState(isLoading = false, error = "An unexpected error occurred")
            }
        }
    }

    // Helper function to create the list of category items for the UI.
    private suspend fun generateDynamicCategoryUiList(
        budgetId: Long?, // The ID of the overall budget for this month (if set)
        categorySpendingList: List<CategorySpending>, // How much was spent in each category
        relevantCategoryNames: List<String> // The categories to actually display
    ): List<HomeCategoryItemUiState> {
        // Get the specific limits set for each category (if a budget exists).
        val categoryLimits = if (budgetId != null) {
            budgetRepository.getCategoryBudgets(budgetId).firstOrNull() ?: emptyList()
        } else {
            emptyList()
        }
        // Put limits into a Map for easy lookup (Category Name -> Limit Amount).
        val limitsMap = categoryLimits.associate { it.categoryName to it.allocatedAmount }

        // Put spending into a Map for easy lookup (Category Name -> Spent Amount).
        val spendingMap = categorySpendingList.associate { it.categoryName to it.total }
        
        // Create a UI item for each relevant category.
        return relevantCategoryNames.map { categoryName ->
            val spent = spendingMap[categoryName] ?: BigDecimal.ZERO
            val limit = limitsMap[categoryName] ?: BigDecimal.ZERO

            // Calculate the progress percentage (0-100).
            val progress = if (limit > BigDecimal.ZERO) {
                (spent.divide(limit, 2, RoundingMode.HALF_UP) * BigDecimal(100)).toInt().coerceIn(0, 100)
            } else {
                0 // Show 0% if no limit is set for this category.
            }

            // Create the UI state object for this category row.
            HomeCategoryItemUiState(
                name = categoryName,
                progress = progress,
                percentageText = "$progress%",
                iconResId = getIconForCategory(categoryName) // Get the appropriate icon.
            )
        }
        .sortedBy { it.name } // Sort the list alphabetically.
    }

    // Helper function to prepare data for the daily spending bar chart (last 7 days).
    private fun generateDailySpendingChartData(expenses: List<ExpenseEntity>): Pair<List<BarEntry>, List<String>> {
        val entries = ArrayList<BarEntry>() // List of bar heights
        val labels = ArrayList<String>() // List of labels for the bars (days)
        val daysToShow = 7
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        // Map to store total spending for each of the last 7 days (0 = 6 days ago, 6 = today).
        val dailyTotals = mutableMapOf<Int, BigDecimal>()

        val cal = Calendar.getInstance()
        val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val todayYear = cal.get(Calendar.YEAR)

        // Initialize totals to zero for the last 7 days.
        for (i in 0 until daysToShow) {
            dailyTotals[i] = BigDecimal.ZERO
        }

        // Go through each expense in the list.
        expenses.forEach { expense ->
            cal.time = expense.date
            val expenseDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val expenseYear = cal.get(Calendar.YEAR)

            // Figure out how many days ago this expense occurred.
            val daysDiff = DateUtils.calculateDaysDifference(expenseYear, expenseDayOfYear, todayYear, todayDayOfYear)

            // If the expense was within the last 7 days...
            if (daysDiff in 0 until daysToShow) {
                // Calculate the map index (0 for 6 days ago, up to 6 for today).
                val index = daysToShow - 1 - daysDiff
                // Add the expense amount to that day's total.
                dailyTotals[index] = (dailyTotals[index] ?: BigDecimal.ZERO) + expense.amount
            }
        }

        // Create the BarEntry objects and corresponding labels for the chart.
        cal.timeInMillis = System.currentTimeMillis() // Use current time
        for (i in (daysToShow - 1) downTo 0) {
            val dayIndex = daysToShow - 1 - i // Index for the dailyTotals map (0 to 6)
            // Get the calendar day (Sun, Mon, etc.) for the label.
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            // Add the bar height (total spending) and label.
            entries.add(BarEntry(dayIndex.toFloat(), (dailyTotals[dayIndex] ?: BigDecimal.ZERO).toFloat()))
            labels.add(dayLabels[dayOfWeek - 1])
            // Move calendar back one day for the next iteration.
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        // Return the prepared chart data.
        return Pair(entries.reversed(), labels.reversed()) // Reverse to show chronologically
    }

    // Simple helper to map category names to drawable icons.
    private fun getIconForCategory(categoryName: String): Int {
        return when (categoryName.lowercase()) {
            "food & dining" -> R.drawable.ic_category_food
            "transport", "gas", "car" -> R.drawable.ic_category_transport
            "shopping", "clothes" -> R.drawable.ic_category_shopping
            "utilities", "rent", "housing" -> R.drawable.ic_category_utilities
            else -> R.drawable.ic_category_other
        }
    }

    // NOTE: Date helper functions were removed as they are now expected
    // to be in a separate DateUtils object.

    // Add functions for actions like refresh if needed
} 