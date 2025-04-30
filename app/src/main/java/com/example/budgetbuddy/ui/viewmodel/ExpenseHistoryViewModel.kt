package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.adapter.HistoryListItem
import com.example.budgetbuddy.data.repository.ExpenseRepository
import com.example.budgetbuddy.model.ExpenseItemUi
import com.example.budgetbuddy.util.DateUtils
import com.example.budgetbuddy.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import android.util.Log

sealed class ExpenseHistoryUiState {
    object Loading : ExpenseHistoryUiState()
    data class Success(val items: List<HistoryListItem>) : ExpenseHistoryUiState()
    data class Error(val message: String) : ExpenseHistoryUiState()
    object Empty : ExpenseHistoryUiState()
}

@HiltViewModel
class ExpenseHistoryViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Hold selected date range state
    private val _selectedStartDate = MutableStateFlow<Date?>(null)
    private val _selectedEndDate = MutableStateFlow<Date?>(null)

    private val _uiState = MutableStateFlow<ExpenseHistoryUiState>(ExpenseHistoryUiState.Loading)
    val uiState: StateFlow<ExpenseHistoryUiState> = _uiState.asStateFlow()

    init {
        loadExpenses()
    }

    // Function called by Fragment to update the date range
    fun setDateRange(startDate: Date?, endDate: Date?) {
        // Adjust endDate to be end of the day for inclusive query
        val adjustedEndDate = endDate?.let {
            Log.d("ExpenseHistoryVM", "Setting date range: Start=$startDate, End=$endDate (Adjusted End=$it)")
            Calendar.getInstance().apply {
                time = it
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time
        }

        _selectedStartDate.value = startDate
        _selectedEndDate.value = adjustedEndDate
        loadExpenses() // Reload expenses with the new date range
    }

    private fun loadExpenses() {
        val userId = sessionManager.getUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            _uiState.value = ExpenseHistoryUiState.Error("User not logged in")
            return
        }

        val startDate = _selectedStartDate.value
        val endDate = _selectedEndDate.value

        viewModelScope.launch {
            Log.d("ExpenseHistoryVM", "Loading expenses for range: ${startDate?.time} to ${endDate?.time}")
            // Choose the correct repository function based on whether dates are set
            val expensesFlow = if (startDate != null && endDate != null) {
                expenseRepository.getExpensesBetween(userId, startDate, endDate)
            } else {
                // Default behavior: load all expenses if no range is selected
                // Or implement a default range like last 30 days
                expenseRepository.getAllExpenses(userId)
            }

            expensesFlow
                .map { expenses ->
                    Log.d("ExpenseHistoryVM", "Expenses received from DAO: ${expenses.size}")
                    // Map ExpenseEntity to ExpenseItemUi and group by date
                    val uiExpenses = expenses.map { entity ->
                        ExpenseItemUi(
                            id = entity.expenseId,
                            amount = entity.amount,
                            category = entity.categoryName,
                            date = entity.date,
                            description = entity.notes ?: "",
                            receiptPath = entity.receiptPath
                        )
                    }
                    ExpenseHistoryUiState.Success(groupExpensesByDate(uiExpenses))
                }
                .onStart { _uiState.value = ExpenseHistoryUiState.Loading }
                .catch { e -> _uiState.value = ExpenseHistoryUiState.Error(e.message ?: "Failed to load expenses") }
                .collect { state -> _uiState.value = state }
        }
    }

    // Function to group expenses and add date headers
    private fun groupExpensesByDate(expenses: List<ExpenseItemUi>): List<HistoryListItem> {
        if (expenses.isEmpty()) return emptyList()

        val groupedList = mutableListOf<HistoryListItem>()
        // Sort by date descending (most recent first)
        val sortedExpenses = expenses.sortedByDescending { it.date }

        var lastHeaderDate: Calendar? = null

        sortedExpenses.forEach { expense ->
            val expenseCalendar = Calendar.getInstance().apply { time = expense.date }

            // Check if we need to add a new date header
            if (lastHeaderDate == null || !DateUtils.isSameDay(expenseCalendar, lastHeaderDate)) {
                val dateText = DateUtils.getRelativeDateString(expenseCalendar)
                groupedList.add(HistoryListItem.DateHeader(dateText))
                lastHeaderDate = expenseCalendar
            }
            groupedList.add(HistoryListItem.ExpenseEntry(expense))
        }
        return groupedList
    }

    // TODO: Add functions for filtering (date range, category, etc.)
    // e.g., fun setDateRange(startDate: Date, endDate: Date) { loadExpenses() }

} 