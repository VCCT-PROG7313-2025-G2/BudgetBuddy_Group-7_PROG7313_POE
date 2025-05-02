package com.example.budgetbuddy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.db.pojo.CategorySpending
import com.example.budgetbuddy.data.db.entity.ExpenseEntity
import com.example.budgetbuddy.data.repository.ExpenseRepository
import com.example.budgetbuddy.data.repository.BudgetRepository
import com.example.budgetbuddy.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import android.graphics.Color
import java.text.NumberFormat

// Define Enums
// Enum to track whether the category legend should show percentages or amounts.
enum class CategoryDisplayMode {
   PERCENTAGE, AMOUNT
}

// Define ChartData or import if defined elsewhere
// This might be an older definition, the specific chart data is now part of ReportsUiState.
/* data class ChartData(
    val entries: List<Any>,
    val labels: List<String>? = null,
    val colors: List<Int>? = null
) */

// Holds all data needed for the Reports screen UI.
data class ReportsUiState(
    val isLoading: Boolean = true,
    // Current mode for displaying category values in the legend.
    val categoryDisplayMode: CategoryDisplayMode = CategoryDisplayMode.PERCENTAGE,
    // Total spending for the current period (week).
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val averageBudget: BigDecimal = BigDecimal.ZERO, // Placeholder, currently shows monthly budget.
    // Older chart data fields, replaced by specific fields below.
    // val spendingByCategoryChart: ChartData? = null,
    // val spendingTrendChart: ChartData? = null,
    val error: String? = null,
    // --- Fields actually used by ReportsFragment ---
    // Calendar representing the start of the period (used for month display).
    val selectedDate: Calendar = Calendar.getInstance(),
    // Formatted text for the month selector (e.g., "August 2024").
    val selectedMonthYearText: String = "",
    // Text showing spending change compared to the previous month.
    val spendingChangeText: String = "",
    // Data points for the category spending pie chart.
    val pieChartData: List<PieEntry> = emptyList(),
    // Colors used for the pie chart slices and legend dots.
    val pieChartColors: List<Int> = emptyList(),
    // Raw spending data per category (used for toggling legend display).
    val categorySpendingData: List<Pair<String, BigDecimal>> = emptyList(),
    // Formatted data for the custom pie chart legend (pairs of Category Name and Value String).
    val pieChartLegend: List<Pair<String, String>> = emptyList(),
    // Data for the weekly spending bar chart (list of bar heights and day labels).
    val barChartData: Pair<List<BarEntry>, List<String>>? = null
)

// Marks this ViewModel for Hilt injection.
@HiltViewModel
class ReportsViewModel @Inject constructor(
    // Inject repositories for accessing expense and budget data.
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Holds the current UI state for the Reports screen.
    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    // Helper to get the current user's ID.
    private fun getCurrentUserId(): Long = sessionManager.getUserId()

    // Initialize by loading data for the default (Weekly) period.
    init {
        loadReportData() // Load weekly data initially.
    }

    // Main function to load data (always WEEKLY now).
    private fun loadReportData() {
        val userId = getCurrentUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            Log.e("ReportsViewModel", "Cannot load data, no user logged in")
            _uiState.value = ReportsUiState(isLoading = false, error = "Please log in.")
            return
        }

        // Set loading state.
        _uiState.update { it.copy(isLoading = true) }
        // Launch background coroutine.
        viewModelScope.launch {
            try {
                // Get start and end dates for the current week.
                val (startDate, endDate) = getDatesForCurrentWeek()
                // Determine the month string for fetching the monthly budget (used as reference).
                val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthYear = monthYearFormat.format(startDate)

                // Define flows to get data from repositories.
                val weeklyExpensesFlow = expenseRepository.getExpensesBetween(userId, startDate, endDate)
                val weeklyCategorySpendingFlow = expenseRepository.getSpendingByCategoryBetween(userId, startDate, endDate)
                val monthlyBudgetFlow = budgetRepository.getBudgetForMonth(userId, currentMonthYear)

                // Combine the latest results from the flows.
                combine(
                    weeklyExpensesFlow,
                    weeklyCategorySpendingFlow,
                    monthlyBudgetFlow
                ) { expenses, categorySpendingList, budgetForMonth ->

                    // Process the raw data.
                    val totalSpending = expenses.sumOf { it.amount } // Sum expenses for the week.
                    val avgBudget = budgetForMonth?.totalAmount ?: BigDecimal.ZERO // Use total monthly budget here.
                    val (pieEntries, rawCategoryData) = processCategoryData(categorySpendingList, totalSpending)
                    val barData = processWeeklyBarChartData(expenses, startDate) // Use specific weekly processor

                    // Create the new UI state object.
                    ReportsUiState(
                        isLoading = false,
                        categoryDisplayMode = _uiState.value.categoryDisplayMode, // Keep existing mode.
                        totalSpending = totalSpending,
                        averageBudget = avgBudget, // Note: Still showing monthly budget here.
                        selectedDate = Calendar.getInstance().apply { time = startDate }, // Represents the week.
                        selectedMonthYearText = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(startDate), // Display month.
                        spendingChangeText = "", // TODO: Calculate weekly change if needed.
                        pieChartData = pieEntries,
                        pieChartColors = generateGreyShades(pieEntries.size), // Generate colors for pie chart.
                        categorySpendingData = rawCategoryData, // Store raw data for legend toggling.
                        pieChartLegend = formatLegendData(rawCategoryData, _uiState.value.categoryDisplayMode, totalSpending),
                        barChartData = barData, // Weekly bar chart data.
                        error = null
                    )
                }.catch { e: Throwable -> // Handle errors during combine.
                    Log.e("ReportsViewModel", "Error combining weekly flows for user $userId", e)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load report data.") }
                }.collect { newState: ReportsUiState -> // Update the main UI state.
                    _uiState.value = newState
                }
            } catch (e: Exception) { // Catch other errors.
                Log.e("ReportsViewModel", "Error in loadReportData launch block", e)
                _uiState.update { it.copy(isLoading = false, error = "An unexpected error occurred.") }
            }
        }
    }

    // Calculates the start and end dates for the current week (Sunday-Saturday).
    private fun getDatesForCurrentWeek(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.SUNDAY // Set week start day
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        setCalendarToStartOfDay(calendar)
        val startDate = calendar.time
        calendar.add(Calendar.DAY_OF_WEEK, 6) // Add 6 days to get Saturday.
        setCalendarToEndOfDay(calendar)
        val endDate = calendar.time
        return Pair(startDate, endDate)
    }

    // Calculates the start and end dates for the week containing the start of the given calendar's month.
    private fun getDatesForWeekStartingInMonth(baseCalendar: Calendar): Pair<Date, Date> {
        val calendar = baseCalendar.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1) // Go to start of the month
        calendar.firstDayOfWeek = Calendar.SUNDAY // Set week start day
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek) // Go to start of that week
        setCalendarToStartOfDay(calendar)
        val startDate = calendar.time
        calendar.add(Calendar.DAY_OF_WEEK, 6) // Add 6 days to get Saturday.
        setCalendarToEndOfDay(calendar)
        val endDate = calendar.time
        return Pair(startDate, endDate)
    }

    // Processes the list of category spending into data suitable for the PieChart.
    private fun processCategoryData(categorySpendingList: List<CategorySpending>, totalSpending: BigDecimal): Pair<List<PieEntry>, List<Pair<String, BigDecimal>>> {
        // Handle cases with no spending.
        if (totalSpending <= BigDecimal.ZERO || categorySpendingList.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }
        val pieEntries = mutableListOf<PieEntry>()
        val categoryData = mutableListOf<Pair<String, BigDecimal>>() // Store raw data too.
        categorySpendingList.forEach { categorySpending ->
            // Calculate percentage, create PieEntry (value = percentage, label = category name).
            val percentage = (categorySpending.total.divide(totalSpending, 4, RoundingMode.HALF_UP) * BigDecimal(100))
            val percentageFloat = percentage.toFloat()
            // Only include slices large enough to be meaningful.
            if (percentageFloat > 0.5) {
                pieEntries.add(PieEntry(percentageFloat, categorySpending.categoryName))
                categoryData.add(categorySpending.categoryName to categorySpending.total)
            }
        }
        categoryData.sortByDescending { it.second } // Sort raw data by amount for potential use.
        return Pair(pieEntries, categoryData)
    }

    // Processes the list of expenses into data suitable for the Weekly BarChart.
    private fun processWeeklyBarChartData(dailyExpensesList: List<ExpenseEntity>, startDate: Date): Pair<List<BarEntry>, List<String>> {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val calendar = Calendar.getInstance()

        val weeklyTotals = Array(7) { BigDecimal.ZERO } // Array to hold totals for Sun-Sat.
        val dayFormat = SimpleDateFormat("E", Locale.getDefault()) // Format for day labels ("Sun", "Mon", etc.).
        // Create labels for the 7 days starting from the week's start date.
        calendar.time = startDate
        for(i in 0..6) {
            labels.add(dayFormat.format(calendar.time))
            calendar.add(Calendar.DATE, 1)
        }
        // Sum expenses into the correct day's bucket.
        val expenseCalendar = Calendar.getInstance()
        dailyExpensesList.forEach { expense ->
            expenseCalendar.time = expense.date
            val diff = (expense.date.time - startDate.time)
            val dayIndex = (diff / (1000 * 60 * 60 * 24)).toInt()
            if(dayIndex in 0..6) {
                weeklyTotals[dayIndex] += expense.amount
            }
        }
        // Create BarEntry for each day.
        for(i in 0..6) {
            entries.add(BarEntry(i.toFloat(), weeklyTotals[i].toFloat()))
        }

        return Pair(entries, labels)
    }

    // --- Calendar Helpers --- 
    private fun setCalendarToStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }
    private fun setCalendarToEndOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
    }
    // --- End Calendar Helpers --- 

    // Called when the user clicks the previous/next month buttons.
    fun changeMonth(amount: Int) {
        val currentCalendar = _uiState.value.selectedDate.clone() as Calendar
        currentCalendar.add(Calendar.MONTH, amount)
        // Reload data based on the new month (will fetch the week starting in that month).
        loadReportDataForDate(currentCalendar)
    }

    // Loads data specifically when navigating months (keeps weekly view).
    private fun loadReportDataForDate(calendar: Calendar) {
        val userId = getCurrentUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            _uiState.update { it.copy(isLoading = false, error = "Please log in.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, selectedDate = calendar, error = null) }
        viewModelScope.launch {
            try {
                // Get month text for display.
                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val selectedMonthYearText = dateFormat.format(calendar.time)

                // *** Get dates for the specific WEEK starting in the selected month. ***
                val (startOfWeek, endOfWeek) = getDatesForWeekStartingInMonth(calendar)

                // Get previous *month's* start/end for comparison text.
                val previousMonthCalendar = calendar.clone() as Calendar
                previousMonthCalendar.add(Calendar.MONTH, -1)
                val startOfPreviousMonth = getStartOfMonth(previousMonthCalendar)
                val endOfPreviousMonth = getEndOfMonth(previousMonthCalendar)

                // Fetch data using the calculated WEEKLY range, but previous month for comparison.
                val spendingSelectedWeekDeferred = async { expenseRepository.getTotalSpendingBetween(userId, startOfWeek, endOfWeek).first() }
                val spendingPreviousMonthDeferred = async { expenseRepository.getTotalSpendingBetween(userId, startOfPreviousMonth, endOfPreviousMonth).first() }
                val categorySpendingDeferred = async { expenseRepository.getSpendingByCategoryBetween(userId, startOfWeek, endOfWeek).first() }
                val weeklyExpensesDeferred = async { expenseRepository.getExpensesBetween(userId, startOfWeek, endOfWeek).first() }

                // Await all data fetching.
                val totalSpendingSelected = spendingSelectedWeekDeferred.await() ?: BigDecimal.ZERO
                val totalSpendingPrevious = spendingPreviousMonthDeferred.await() ?: BigDecimal.ZERO
                val categorySpendingList = categorySpendingDeferred.await()
                val weeklyExpensesList = weeklyExpensesDeferred.await()

                // Process the fetched data.
                val spendingChangeText = calculateSpendingChange(totalSpendingSelected, totalSpendingPrevious)
                val (pieChartData, rawCategoryData) = processCategoryData(categorySpendingList, totalSpendingSelected)
                val pieChartLegend = formatLegendData(rawCategoryData, _uiState.value.categoryDisplayMode, totalSpendingSelected)
                val barChartData = processWeeklyBarChartData(weeklyExpensesList, startOfWeek) // Use specific weekly processor
                val pieChartColors = generateGreyShades(pieChartData.size)

                // Update the UI State.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedDate = calendar, // Keep representing the month for display.
                        selectedMonthYearText = selectedMonthYearText,
                        totalSpending = totalSpendingSelected, // Show WEEKLY total.
                        spendingChangeText = spendingChangeText, // Compare WEEK vs PREVIOUS MONTH.
                        pieChartData = pieChartData,
                        pieChartColors = pieChartColors,
                        categorySpendingData = rawCategoryData,
                        pieChartLegend = pieChartLegend,
                        barChartData = barChartData,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("ReportsViewModel", "Error in loadReportDataForDate", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to load specific month report data.") }
            }
        }
    }

    // Calculates the percentage change text comparing current spending to previous period.
    private fun calculateSpendingChange(currentSpending: BigDecimal, previousSpending: BigDecimal): String {
        if (previousSpending <= BigDecimal.ZERO) {
            return if (currentSpending > BigDecimal.ZERO) "↑ from last month" else "" // No previous data or increase from zero
        }
        val change = ((currentSpending - previousSpending).divide(previousSpending, 4, RoundingMode.HALF_UP) * BigDecimal(100))
        val percentage = change.setScale(1, RoundingMode.HALF_UP)
        return when {
            percentage > BigDecimal.ZERO -> "↑ $percentage% from last month"
            percentage < BigDecimal.ZERO -> "↓ ${percentage.abs()}% from last month"
            else -> "↔ No change from last month"
        }
    }

    // --- More Date Helpers (For specific month calculations) ---
    private fun getStartOfMonth(calendar: Calendar): Date {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        setCalendarToStartOfDay(cal)
        return cal.time
    }
    private fun getEndOfMonth(calendar: Calendar): Date {
         val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        setCalendarToEndOfDay(cal)
        return cal.time
    }
    // --- End Date Helpers ---

    // Public function to allow refreshing data manually (e.g., pull-to-refresh).
    fun refreshData() {
        loadReportData()
    }

    // Generates a list of grey colors for the pie chart.
    private fun generateGreyShades(count: Int): List<Int> {
        if (count <= 0) return emptyList()
        val shades = mutableListOf<Int>()
        val baseGreys = listOf(
            Color.rgb(50, 50, 50), // Very Dark Grey
            Color.DKGRAY,
            Color.GRAY,
            Color.LTGRAY,
            Color.rgb(220, 220, 220) // Very Light Grey
        )
        for (i in 0 until count) {
            shades.add(baseGreys[i % baseGreys.size])
        }
        return shades
    }

    // --- Report Generation Logic --- 
    // Creates a plain text summary of the current report data.
    fun generateReportContent(): String? {
        val state = _uiState.value
        if (state.isLoading || state.error != null) {
            Log.w("ReportsViewModel", "Cannot generate report while loading or in error state.")
            return null
        }

        val builder = StringBuilder()
        builder.appendLine("Budget Buddy Report")
        builder.appendLine("Period Covered: Week starting ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(getDatesForCurrentWeek().first)}")
        builder.appendLine("(Month Displayed: ${state.selectedMonthYearText})") // Clarify which month is shown
        builder.appendLine("=====================================")
        builder.appendLine("Total Spending (Week): ${formatCurrency(state.totalSpending)}")
        if (state.spendingChangeText.isNotEmpty()) {
             builder.appendLine("Change (Week vs Prev Month): ${state.spendingChangeText}")
        }
        builder.appendLine()
        builder.appendLine("Spending by Category (Week):")
        builder.appendLine("-------------------------------------")

        if (state.pieChartLegend.isEmpty()) {
            builder.appendLine("No category spending data for this period.")
        } else {
            // Use raw data to show both amount and percentage
            val total = state.totalSpending
            state.categorySpendingData.forEach { (category, amount) ->
                val percentage = if (total > BigDecimal.ZERO) {
                    (amount.divide(total, 4, RoundingMode.HALF_UP) * BigDecimal(100)).setScale(1, RoundingMode.HALF_UP)
                } else BigDecimal.ZERO
                builder.appendLine("- $category: ${formatCurrency(amount)} ($percentage%)")
            }
        }
        // Add weekly breakdown
        builder.appendLine()
        builder.appendLine("Spending by Day (Week):")
        builder.appendLine("-------------------------------------")
        if (state.barChartData == null || state.barChartData.first.isEmpty()) {
            builder.appendLine("No daily spending data for this period.")
        } else {
            state.barChartData.second.forEachIndexed { index, label ->
                val amount = state.barChartData.first.getOrNull(index)?.y?.toBigDecimal() ?: BigDecimal.ZERO
                builder.appendLine("- $label: ${formatCurrency(amount)}")
            }
        }

        builder.appendLine()
        builder.appendLine("=====================================")
        builder.appendLine("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")

        return builder.toString()
    }

    // Helper to format currency.
    private fun formatCurrency(amount: BigDecimal): String {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
    }

    // Toggles the display mode for the pie chart legend (Percentage vs Amount).
    fun toggleCategoryDisplayMode() {
        _uiState.update { currentState ->
            val newMode = if (currentState.categoryDisplayMode == CategoryDisplayMode.PERCENTAGE) {
                CategoryDisplayMode.AMOUNT
            } else {
                CategoryDisplayMode.PERCENTAGE
            }
            // Recalculate the legend text based on the new mode.
            val updatedLegend = formatLegendData(
                categoryData = currentState.categorySpendingData,
                mode = newMode,
                totalSpending = currentState.totalSpending
            )
            // Update the state with the new mode and legend.
            currentState.copy(
                categoryDisplayMode = newMode,
                pieChartLegend = updatedLegend
            )
        }
    }

    // Formats the raw category spending data into strings for the legend.
    private fun formatLegendData(
        categoryData: List<Pair<String, BigDecimal>>,
        mode: CategoryDisplayMode,
        totalSpending: BigDecimal
    ): List<Pair<String, String>> {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        return categoryData.map { (categoryName, amount) ->
            val formattedValue = when (mode) {
                CategoryDisplayMode.PERCENTAGE -> {
                    if (totalSpending > BigDecimal.ZERO) {
                        val percentage = (amount.divide(totalSpending, 4, RoundingMode.HALF_UP) * BigDecimal(100))
                            .setScale(1, RoundingMode.HALF_UP)
                        "$percentage%"
                    } else {
                        "0.0%"
                    }
                }
                CategoryDisplayMode.AMOUNT -> formatter.format(amount)
            }
            categoryName to formattedValue
        }
    }
}