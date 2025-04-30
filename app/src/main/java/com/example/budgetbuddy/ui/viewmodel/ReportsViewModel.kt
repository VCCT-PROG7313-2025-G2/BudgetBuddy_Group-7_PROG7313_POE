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

// Define Period Enum here or import if defined elsewhere
enum class Period {
    DAILY, WEEKLY, MONTHLY // Add YEARLY etc. if needed
}

// Define ChartData or import if defined elsewhere
// This is a placeholder, adapt to your actual chart library needs
data class ChartData(
    val entries: List<Any>, // Use specific chart entry type (PieEntry, BarEntry)
    val labels: List<String>? = null,
    val colors: List<Int>? = null
)

// Define ReportsUiState matching the data needed by the Fragment
data class ReportsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: Period = Period.MONTHLY,
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val averageBudget: BigDecimal = BigDecimal.ZERO, // Example field
    val spendingByCategoryChart: ChartData? = null, // Holds PieChart data
    val spendingTrendChart: ChartData? = null, // Holds BarChart data
    val error: String? = null,
    // Fields from the older version that might be needed by the current Fragment:
    val selectedDate: Calendar = Calendar.getInstance(),
    val selectedMonthYearText: String = "",
    val spendingChangeText: String = "",
    val pieChartData: List<PieEntry> = emptyList(),
    val pieChartColors: List<Int> = emptyList(),
    val pieChartLegend: List<Pair<String, String>> = emptyList(),
    val barChartData: Pair<List<BarEntry>, List<String>>? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(Period.MONTHLY)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod.asStateFlow()

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private fun getCurrentUserId(): Long = sessionManager.getUserId()

    init {
        viewModelScope.launch {
            selectedPeriod.collect { period ->
                loadReportData(period)
            }
        }
    }

    fun setPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    private fun loadReportData(period: Period) {
        val userId = getCurrentUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            Log.e("ReportsViewModel", "Cannot load data, no user logged in")
            _uiState.value = ReportsUiState(isLoading = false, error = "Please log in.")
            return
        }

        _uiState.update { it.copy(isLoading = true, selectedPeriod = period) }
        viewModelScope.launch {
            try {
                val (startDate, endDate) = getDatesForPeriod(period)

                // Use correct repository methods
                // Note: BudgetRepository doesn't have an average budget flow, we fetch the specific month's budget
                val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthYear = monthYearFormat.format(startDate) // Use start date to determine month

                combine(
                    expenseRepository.getExpensesBetween(userId, startDate, endDate),
                    expenseRepository.getSpendingByCategoryBetween(userId, startDate, endDate),
                    budgetRepository.getBudgetForMonth(userId, currentMonthYear) // Get budget for the relevant month
                ) { expenses, categorySpendingList, budgetForMonth ->

                    val totalSpending = expenses.sumOf { it.amount }
                    val avgBudget = budgetForMonth?.totalAmount ?: BigDecimal.ZERO // Use the fetched budget
                    val (pieEntries, legend) = processPieChartData(categorySpendingList, totalSpending)
                    val barData = processBarChartData(expenses, startDate, endDate, period) // Pass dates/period for bar chart processing

                    // Populate the state object correctly
                    ReportsUiState(
                        isLoading = false,
                        selectedPeriod = period,
                        totalSpending = totalSpending,
                        averageBudget = avgBudget, // Populate with actual monthly budget
                        // Populate the fields used by the current Fragment implementation:
                        selectedDate = Calendar.getInstance().apply{ time = startDate }, // Reflect start of period
                        selectedMonthYearText = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(startDate),
                        spendingChangeText = "", // TODO: Recalculate if needed
                        pieChartData = pieEntries,
                        pieChartColors = ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.VORDIPLOM_COLORS.toList(), // Generate colors
                        pieChartLegend = legend,
                        barChartData = barData,
                        error = null
                        // Remove spendingByCategoryChart, spendingTrendChart if replaced by specific fields
                    )
                }.catch { e: Throwable ->
                    Log.e("ReportsViewModel", "Error combining flows for user $userId, period $period", e)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load report data.") }
                }.collect { newState: ReportsUiState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                Log.e("ReportsViewModel", "Error in loadReportData launch block", e)
                _uiState.update { it.copy(isLoading = false, error = "An unexpected error occurred.") }
            }
        }
    }

    private fun getDatesForPeriod(period: Period): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val endDate: Date
        val startDate: Date

        when (period) {
            Period.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                setCalendarToStartOfDay(calendar)
                startDate = calendar.time
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                setCalendarToEndOfDay(calendar)
                endDate = calendar.time
            }
            Period.WEEKLY -> {
                calendar.firstDayOfWeek = Calendar.SUNDAY // Or Monday
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                setCalendarToStartOfDay(calendar)
                startDate = calendar.time
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                setCalendarToEndOfDay(calendar)
                endDate = calendar.time
            }
            Period.DAILY -> {
                 setCalendarToStartOfDay(calendar)
                 startDate = calendar.time
                 setCalendarToEndOfDay(calendar)
                 endDate = calendar.time
            }
        }
        return Pair(startDate, endDate)
    }

    private fun processPieChartData(categorySpendingList: List<CategorySpending>, totalSpending: BigDecimal): Pair<List<PieEntry>, List<Pair<String, String>>> {
         if (totalSpending <= BigDecimal.ZERO || categorySpendingList.isEmpty()) {
             return Pair(emptyList(), emptyList())
         }
         val pieEntries = mutableListOf<PieEntry>()
         val legendItems = mutableListOf<Pair<String, String>>()
         categorySpendingList.forEach { categorySpending ->
             val percentage = (categorySpending.total.divide(totalSpending, 4, RoundingMode.HALF_UP) * BigDecimal(100))
             val percentageFloat = percentage.toFloat()
             if (percentageFloat > 0.5) { // Threshold to avoid tiny slices
                 pieEntries.add(PieEntry(percentageFloat, categorySpending.categoryName))
                 legendItems.add(categorySpending.categoryName to "${percentage.setScale(1, RoundingMode.HALF_UP)}%")
             }
         }
         legendItems.sortByDescending { it.second.removeSuffix("%").toFloatOrNull() ?: 0f }
         return Pair(pieEntries, legendItems)
     }

     private fun processBarChartData(dailyExpensesList: List<ExpenseEntity>, startDate: Date, endDate: Date, period: Period): Pair<List<BarEntry>, List<String>> {
         val entries = ArrayList<BarEntry>()
         val labels = ArrayList<String>()
         val calendar = Calendar.getInstance()

         when (period) {
             Period.MONTHLY -> {
                 calendar.time = startDate
                 val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                 val dailyTotals = Array(daysInMonth + 1) { BigDecimal.ZERO } // Index 1 = Day 1
                 val expenseCalendar = Calendar.getInstance()
                 dailyExpensesList.forEach { expense ->
                     expenseCalendar.time = expense.date
                     val dayOfMonth = expenseCalendar.get(Calendar.DAY_OF_MONTH)
                     if (dayOfMonth in 1..daysInMonth) {
                         dailyTotals[dayOfMonth] += expense.amount
                     }
                 }
                 for (day in 1..daysInMonth) {
                     entries.add(BarEntry(day.toFloat(), dailyTotals[day].toFloat()))
                     labels.add(if (daysInMonth <= 10 || day % 5 == 0 || day == 1 || day == daysInMonth) day.toString() else "") // Smart labeling
                 }
             }
             // TODO: Implement similar logic for WEEKLY and DAILY periods if needed
             Period.WEEKLY, Period.DAILY -> {
                  // Example: Logic for weekly (adapt as needed)
                  val weeklyTotals = Array(7) { BigDecimal.ZERO } // 0=Sun, 1=Mon...
                  val dayFormat = SimpleDateFormat("E", Locale.getDefault()) // Short day name
                  calendar.time = startDate
                  for(i in 0..6) {
                       labels.add(dayFormat.format(calendar.time))
                       calendar.add(Calendar.DATE, 1)
                  }
                  val expenseCalendar = Calendar.getInstance()
                   dailyExpensesList.forEach { expense ->
                       expenseCalendar.time = expense.date
                       // Map expense date to correct index (0-6) based on start date
                       val diff = (expense.date.time - startDate.time)
                       val dayIndex = (diff / (1000 * 60 * 60 * 24)).toInt()
                       if(dayIndex in 0..6) {
                           weeklyTotals[dayIndex] += expense.amount
                       }
                   }
                   for(i in 0..6) {
                        entries.add(BarEntry(i.toFloat(), weeklyTotals[i].toFloat()))
                   }
             }
         }
         return Pair(entries, labels)
     }

    // Helper to set calendar to start of the day
    private fun setCalendarToStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    // Helper to set calendar to end of the day
    private fun setCalendarToEndOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
    }

    fun changeMonth(amount: Int) {
        val currentCalendar = _uiState.value.selectedDate.clone() as Calendar
        currentCalendar.add(Calendar.MONTH, amount)
        loadReportDataForDate(currentCalendar)
    }

    private fun loadReportDataForDate(calendar: Calendar) {
        val userId = getCurrentUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            _uiState.update { it.copy(isLoading = false, error = "Please log in.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, selectedDate = calendar, error = null) }
        viewModelScope.launch {
            try {
                // Calculate dates based on the passed calendar
                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val selectedMonthYearText = dateFormat.format(calendar.time)
                val startOfMonth = getStartOfMonth(calendar)
                val endOfMonth = getEndOfMonth(calendar)
                val previousMonthCalendar = calendar.clone() as Calendar
                previousMonthCalendar.add(Calendar.MONTH, -1)
                val startOfPreviousMonth = getStartOfMonth(previousMonthCalendar)
                val endOfPreviousMonth = getEndOfMonth(previousMonthCalendar)

                // Fetch data concurrently (as before)
                val spendingSelectedMonthDeferred = async { expenseRepository.getTotalSpendingBetween(userId, startOfMonth, endOfMonth).first() }
                val spendingPreviousMonthDeferred = async { expenseRepository.getTotalSpendingBetween(userId, startOfPreviousMonth, endOfPreviousMonth).first() }
                val categorySpendingDeferred = async { expenseRepository.getSpendingByCategoryBetween(userId, startOfMonth, endOfMonth).first() }
                val dailyExpensesDeferred = async { expenseRepository.getExpensesBetween(userId, startOfMonth, endOfMonth).first() }

                // Await results
                val totalSpendingSelected = spendingSelectedMonthDeferred.await() ?: BigDecimal.ZERO
                val totalSpendingPrevious = spendingPreviousMonthDeferred.await() ?: BigDecimal.ZERO
                val categorySpendingList = categorySpendingDeferred.await()
                val dailyExpensesList = dailyExpensesDeferred.await()

                // Process data
                val spendingChangeText = calculateSpendingChange(totalSpendingSelected, totalSpendingPrevious)
                val (pieChartData, pieChartLegend) = processPieChartData(categorySpendingList, totalSpendingSelected)
                // Call processBarChartData with correct parameters - assuming Monthly period for this specific loader
                val barChartData = processBarChartData(dailyExpensesList, startOfMonth, endOfMonth, Period.MONTHLY)
                val pieChartColors = com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS.toList() + com.github.mikephil.charting.utils.ColorTemplate.VORDIPLOM_COLORS.toList()

                // Update state with all necessary fields
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedDate = calendar,
                        selectedMonthYearText = selectedMonthYearText,
                        totalSpending = totalSpendingSelected,
                        spendingChangeText = spendingChangeText,
                        pieChartData = pieChartData,
                        pieChartColors = pieChartColors,
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

    private fun calculateSpendingChange(currentMonthSpending: BigDecimal, previousMonthSpending: BigDecimal): String {
        if (previousMonthSpending <= BigDecimal.ZERO) {
            return if (currentMonthSpending > BigDecimal.ZERO) "↑ from last month" else "" // No previous data or increase from zero
        }
        val change = ((currentMonthSpending - previousMonthSpending).divide(previousMonthSpending, 4, RoundingMode.HALF_UP) * BigDecimal(100))
        val percentage = change.setScale(1, RoundingMode.HALF_UP)
        return when {
            percentage > BigDecimal.ZERO -> "↑ $percentage% from last month"
            percentage < BigDecimal.ZERO -> "↓ ${percentage.abs()}% from last month"
            else -> "↔ No change from last month"
        }
    }

    // --- Date Helper Functions (adjust for specific calendar) ---
    private fun getStartOfMonth(calendar: Calendar): Date {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun getEndOfMonth(calendar: Calendar): Date {
         val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }

    fun refreshData() {
        loadReportData(selectedPeriod.value)
    }
} 