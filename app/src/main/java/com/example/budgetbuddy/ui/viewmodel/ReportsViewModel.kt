package com.example.budgetbuddy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.db.pojo.CategorySpending
import com.example.budgetbuddy.data.db.entity.ExpenseEntity
import com.example.budgetbuddy.data.repository.ExpenseRepository
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

data class ReportsUiState(
    val selectedDate: Calendar = Calendar.getInstance(),
    val selectedMonthYearText: String = "",
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val spendingChangeText: String = "", // e.g., "↑ 12.5% from last month"
    val pieChartData: List<PieEntry> = emptyList(),
    val pieChartColors: List<Int> = emptyList(),
    val pieChartLegend: List<Pair<String, String>> = emptyList(), // Pair(CategoryName, PercentageText)
    val barChartData: Pair<List<BarEntry>, List<String>>? = null, // Pair(Entries, Labels)
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
    // TODO: Inject UserRepository if needed
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    // TODO: Replace with actual user ID retrieval
    private val currentUserId: Long = 1L

    init {
        loadReportDataForDate(Calendar.getInstance())
    }

    fun changeMonth(amount: Int) {
        val currentCalendar = _uiState.value.selectedDate.clone() as Calendar
        currentCalendar.add(Calendar.MONTH, amount)
        // Prevent going into the future? Optional.
        // if (currentCalendar.after(Calendar.getInstance())) return
        loadReportDataForDate(currentCalendar)
    }

    private fun loadReportDataForDate(calendar: Calendar) {
        _uiState.update { it.copy(isLoading = true, selectedDate = calendar, error = null) }
        viewModelScope.launch {
            try {
                val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val selectedMonthYearText = dateFormat.format(calendar.time)

                // Calculate date ranges
                val startOfMonth = getStartOfMonth(calendar)
                val endOfMonth = getEndOfMonth(calendar)

                val previousMonthCalendar = calendar.clone() as Calendar
                previousMonthCalendar.add(Calendar.MONTH, -1)
                val startOfPreviousMonth = getStartOfMonth(previousMonthCalendar)
                val endOfPreviousMonth = getEndOfMonth(previousMonthCalendar)

                // Fetch data concurrently
                val spendingSelectedMonthDeferred = async { expenseRepository.getTotalSpendingBetween(currentUserId, startOfMonth, endOfMonth).first() }
                val spendingPreviousMonthDeferred = async { expenseRepository.getTotalSpendingBetween(currentUserId, startOfPreviousMonth, endOfPreviousMonth).first() }
                val categorySpendingDeferred = async { expenseRepository.getSpendingByCategoryBetween(currentUserId, startOfMonth, endOfMonth).first() }
                val dailyExpensesDeferred = async { expenseRepository.getExpensesBetween(currentUserId, startOfMonth, endOfMonth).first() }

                // Await results
                val totalSpendingSelected = spendingSelectedMonthDeferred.await() ?: BigDecimal.ZERO
                val totalSpendingPrevious = spendingPreviousMonthDeferred.await() ?: BigDecimal.ZERO
                val categorySpendingList = categorySpendingDeferred.await()
                val dailyExpensesList = dailyExpensesDeferred.await()

                // Process data
                val spendingChangeText = calculateSpendingChange(totalSpendingSelected, totalSpendingPrevious)
                val (pieChartData, pieChartLegend) = processPieChartData(categorySpendingList, totalSpendingSelected)
                val barChartData = processBarChartData(dailyExpensesList, calendar)
                val pieChartColors = ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.VORDIPLOM_COLORS.toList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
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
                Log.e("ReportsViewModel", "Error loading report data", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to load report data") }
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

    private fun processPieChartData(categorySpendingList: List<CategorySpending>, totalSpending: BigDecimal): Pair<List<PieEntry>, List<Pair<String, String>>> {
        if (totalSpending <= BigDecimal.ZERO || categorySpendingList.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        val pieEntries = mutableListOf<PieEntry>()
        val legendItems = mutableListOf<Pair<String, String>>()

        categorySpendingList.forEach { categorySpending ->
            val percentage = (categorySpending.total.divide(totalSpending, 4, RoundingMode.HALF_UP) * BigDecimal(100))
            val percentageFloat = percentage.toFloat()
            if (percentageFloat > 0.5) { // Only include slices > 0.5%
                 pieEntries.add(PieEntry(percentageFloat, categorySpending.categoryName))
                 legendItems.add(categorySpending.categoryName to "${percentage.setScale(1, RoundingMode.HALF_UP)}%")
            }
        }
        // Sort legend by percentage descending
        legendItems.sortByDescending { it.second.removeSuffix("%").toFloatOrNull() ?: 0f }
        return Pair(pieEntries, legendItems)
    }

     private fun processBarChartData(dailyExpensesList: List<ExpenseEntity>, selectedMonthCalendar: Calendar): Pair<List<BarEntry>, List<String>> {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val daysInMonth = selectedMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
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
            // Add labels (e.g., only for specific days if too many)
            labels.add(if (daysInMonth <= 10 || day % 5 == 0 || day == 1 || day == daysInMonth) day.toString() else "")
        }

        return Pair(entries, labels)
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
} 