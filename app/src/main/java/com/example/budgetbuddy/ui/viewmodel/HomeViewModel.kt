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

// --- UI State Definitions (Keep as is) --- 
data class HomeUiState(
    val greeting: String = "",
    val budgetTotal: BigDecimal = BigDecimal.ZERO,
    val budgetSpent: BigDecimal = BigDecimal.ZERO,
    val budgetProgress: Int = 0, // 0-100
    val dailySpendingData: Pair<List<BarEntry>, List<String>>? = null,
    val budgetCategories: List<HomeCategoryItemUiState> = emptyList(),
    val leaderboardPositionText: String = "", // To show user's rank
    val isLoading: Boolean = true,
    val error: String? = null
)

data class HomeCategoryItemUiState(
    val name: String,
    val progress: Int,
    val percentageText: String,
    val iconResId: Int
)
// --- End UI State Definitions ---

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val rewardsRepository: RewardsRepository, // Inject RewardsRepository
    private val sessionManager: SessionManager // Inject SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Get the current user ID from SessionManager
    private fun getCurrentUserId(): Long = sessionManager.getUserId()

    init {
        loadHomeScreenData()
    }

    private fun loadHomeScreenData() {
        val userId = getCurrentUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            Log.e("HomeViewModel", "Cannot load data, no user logged in")
            _uiState.value = HomeUiState(isLoading = false, error = "Please log in.")
            return
        }

        viewModelScope.launch {
            try {
                // Get user for greeting
                val userFlow = userRepository.getUser(userId)

                // Get budget and spending for the current month
                val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthYear = monthYearFormat.format(Date())
                val startOfMonthDate = getStartOfMonth() // Keep as Date
                val endOfMonthDate = getEndOfMonth()   // Keep as Date

                val budgetFlow = budgetRepository.getBudgetForMonth(userId, currentMonthYear)
                val spentFlow = expenseRepository.getTotalSpendingBetween(userId, startOfMonthDate, endOfMonthDate).map { it ?: BigDecimal.ZERO }
                val categorySpendingFlow = expenseRepository.getSpendingByCategoryBetween(userId, startOfMonthDate, endOfMonthDate)

                // Get data for daily spending chart (last 7 days)
                val weekStartDate = getStartOfWeek().time
                val todayEndDate = getEndOfDay().time
                val dailyExpensesFlow = expenseRepository.getExpensesBetween(userId, Date(weekStartDate), Date(todayEndDate))

                // Get the dynamic list of relevant category names for the current month
                val relevantCategoriesFlow = budgetRepository.getRelevantCategoryNamesForPeriod(userId, startOfMonthDate, endOfMonthDate)

                // Combine flows to build the UI State
                combine(
                    userFlow,               // Flow<UserEntity?>
                    budgetFlow,             // Flow<BudgetEntity?>
                    spentFlow,              // Flow<BigDecimal>
                    categorySpendingFlow,   // Flow<List<CategorySpending>>
                    dailyExpensesFlow,      // Flow<List<ExpenseEntity>>
                    rewardsRepository.getFullLeaderboard(), // Leaderboard flow
                    relevantCategoriesFlow  // Flow<List<String>>
                    // TODO: Add rewards flow later
                ) { flows -> // Change to single parameter (array)
                    // Access and cast values from the flows array
                    val user = flows[0] as com.example.budgetbuddy.data.db.entity.UserEntity?
                    val budget = flows[1] as com.example.budgetbuddy.data.db.entity.BudgetEntity?
                    val spent = flows[2] as java.math.BigDecimal
                    val categorySpendingList = flows[3] as List<com.example.budgetbuddy.data.db.pojo.CategorySpending>
                    val weekExpenses = flows[4] as List<com.example.budgetbuddy.data.db.entity.ExpenseEntity>
                    val leaderboard = flows[5] as List<com.example.budgetbuddy.model.UserWithPoints>
                    val relevantCategoryNames = flows[6] as List<String>

                    val greeting = user?.name?.substringBefore(" ")?.let { "Hi, $it" } ?: "Welcome"
                    val budgetTotal = budget?.totalAmount ?: BigDecimal.ZERO
                    val budgetProgress = if (budgetTotal > BigDecimal.ZERO) {
                        (spent.divide(budgetTotal, 2, RoundingMode.HALF_UP) * BigDecimal(100)).toInt().coerceIn(0, 100)
                    } else 0

                    // Generate category UI list using the dynamic names
                    val categoryUiList = generateDynamicCategoryUiList(budget?.budgetId, categorySpendingList, relevantCategoryNames)
                    val dailySpendingData = generateDailySpendingChartData(weekExpenses)

                    // Calculate leaderboard position
                    val userPosition = leaderboard.indexOfFirst { it.user.userId == userId }
                    val positionText = if (userPosition != -1) {
                        "Rank #${userPosition + 1} on Leaderboard" // TODO: Move to strings.xml
                    } else {
                        "Not Ranked Yet" // TODO: Move to strings.xml
                    }

                    HomeUiState(
                        greeting = greeting,
                        budgetTotal = budgetTotal,
                        budgetSpent = spent,
                        budgetProgress = budgetProgress,
                        dailySpendingData = dailySpendingData,
                        budgetCategories = categoryUiList,
                        leaderboardPositionText = positionText,
                        isLoading = false,
                        error = null
                    )
                }.catch { e ->
                    Log.e("HomeViewModel", "Error loading home screen data", e)
                    _uiState.value = HomeUiState(isLoading = false, error = "Failed to load data")
                }.collect { newState ->
                    _uiState.value = newState
                }

            } catch (e: Exception) {
                 Log.e("HomeViewModel", "Exception in loadHomeScreenData", e)
                _uiState.value = HomeUiState(isLoading = false, error = "An unexpected error occurred")
            }
        }
    }

    // Helper to generate category UI list
    private suspend fun generateDynamicCategoryUiList(
        budgetId: Long?,
        categorySpendingList: List<CategorySpending>,
        relevantCategoryNames: List<String> // Use the dynamic list passed in
    ): List<HomeCategoryItemUiState> {
        // Fetch category limits ONLY if a budget exists for the month
        val categoryLimits = if (budgetId != null) {
            budgetRepository.getCategoryBudgets(budgetId).firstOrNull() ?: emptyList()
        } else {
            emptyList()
        }
        val limitsMap = categoryLimits.associate { it.categoryName to it.allocatedAmount }

        // Map spending for easy lookup
        val spendingMap = categorySpendingList.associate { it.categoryName to it.total }
        
        // Build the UI list based on relevant categories
        return relevantCategoryNames.map { categoryName ->
            val spent = spendingMap[categoryName] ?: BigDecimal.ZERO // Look up spending for this category
            val limit = limitsMap[categoryName] ?: BigDecimal.ZERO // Look up the limit for this category

            val progress = if (limit > BigDecimal.ZERO) {
                // Calculate progress normally if limit exists
                (spent.divide(limit, 2, RoundingMode.HALF_UP) * BigDecimal(100)).toInt().coerceIn(0, 100)
            } else {
                // No limit defined for this static category, display progress as 0%
                0 // Display 0% progress if no specific limit is set
            }

            // *** ADD LOGGING HERE ***
            Log.d("HomeViewModel", "Category: $categoryName, Spent: $spent, Limit: $limit, Progress: $progress")

            HomeCategoryItemUiState(
                name = categoryName,
                progress = progress,
                percentageText = "$progress%",
                iconResId = getIconForCategory(categoryName)
            )
        } // No need to sort if using a predefined order
        .sortedBy { it.name } // Sort alphabetically
    }

    // Helper to generate chart data from expenses
    private fun generateDailySpendingChartData(expenses: List<ExpenseEntity>): Pair<List<BarEntry>, List<String>> {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val daysToShow = 7
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val dailyTotals = mutableMapOf<Int, BigDecimal>() // Key: 0=6 days ago, ..., 6=today

        val cal = Calendar.getInstance()
        val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val todayYear = cal.get(Calendar.YEAR)

        // Initialize map for the last 7 days
        for (i in 0 until daysToShow) {
            dailyTotals[i] = BigDecimal.ZERO
        }

        // Sum expenses by day index relative to today
        expenses.forEach { expense ->
            cal.time = expense.date
            val expenseDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val expenseYear = cal.get(Calendar.YEAR)

            val daysDiff = calculateDaysDifference(expenseYear, expenseDayOfYear, todayYear, todayDayOfYear)

            if (daysDiff in 0 until daysToShow) {
                val index = daysToShow - 1 - daysDiff
                dailyTotals[index] = (dailyTotals[index] ?: BigDecimal.ZERO) + expense.amount
            }
        }

        // Create BarEntries and Labels
        cal.timeInMillis = System.currentTimeMillis() // Reset calendar to today
        for (i in (daysToShow - 1) downTo 0) {
            val dayIndex = daysToShow - 1 - i
            val dayMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(i.toLong())
            cal.timeInMillis = dayMillis
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            entries.add(BarEntry(dayIndex.toFloat(), (dailyTotals[dayIndex] ?: BigDecimal.ZERO).toFloat()))
            labels.add(dayLabels[dayOfWeek - 1])
        }

        return Pair(entries, labels)
    }

    // Helper to calculate difference in days (simplified, doesn't perfectly handle year boundaries for > 7 days)
    private fun calculateDaysDifference(year1: Int, dayOfYear1: Int, year2: Int, dayOfYear2: Int): Int {
        if (year1 == year2) {
            return dayOfYear2 - dayOfYear1
        } else if (year2 > year1) {
            // Approximate for simplicity, assumes non-leap year if crossing boundary
             val daysInYear1 = if (Calendar.getInstance().apply { set(Calendar.YEAR, year1) }.getActualMaximum(Calendar.DAY_OF_YEAR) > 365) 366 else 365
            return (daysInYear1 - dayOfYear1) + dayOfYear2 + (year2 - year1 - 1) * 365
        } else {
            return -1 // Date 1 is after Date 2
        }
    }


    // Placeholder icon mapping
    private fun getIconForCategory(categoryName: String): Int {
        return when (categoryName.lowercase()) {
            "food & dining" -> R.drawable.ic_category_food // Updated key
            "transport", "gas", "car" -> R.drawable.ic_category_transport
            "shopping", "clothes" -> R.drawable.ic_category_shopping
            "utilities", "rent", "housing" -> R.drawable.ic_category_utilities // Grouped utilities/housing
            // Removed Entertainment, Personal Care entries as they are not in STATIC_CATEGORIES
            else -> R.drawable.ic_category_other // Catches "other" and any unexpected values
        }
    }

    // --- Date Helper Functions ---
    private fun getStartOfMonth(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun getEndOfMonth(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }

    private fun getStartOfWeek(): Date {
         return Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY // Or Monday, depending on preference
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
             add(Calendar.DAY_OF_YEAR, -6) // Go back 6 days to get start of 7 day window
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun getEndOfDay(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }

    // Add functions for actions like refresh if needed
} 