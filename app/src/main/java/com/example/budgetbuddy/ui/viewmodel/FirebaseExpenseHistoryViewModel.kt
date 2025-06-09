package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.firebase.repository.FirebaseExpenseRepository
import com.example.budgetbuddy.model.ExpenseItemUi
import com.example.budgetbuddy.util.FirebaseSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ExpenseHistoryUiState(
    val expenses: List<ExpenseItemUi> = emptyList(),
    val startDate: Date = getDefaultStartDate(),
    val endDate: Date = Date(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    companion object {
        private fun getDefaultStartDate(): Date {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -30) // Default to last 30 days
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.time
        }
    }
}

@HiltViewModel
class FirebaseExpenseHistoryViewModel @Inject constructor(
    private val expenseRepository: FirebaseExpenseRepository,
    private val sessionManager: FirebaseSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseHistoryUiState())
    val uiState: StateFlow<ExpenseHistoryUiState> = _uiState.asStateFlow()

    init {
        loadExpenses()
    }

    private fun getCurrentUserId(): String = sessionManager.getUserId()

    fun setDateRange(startDate: Date, endDate: Date) {
        _uiState.value = _uiState.value.copy(
            startDate = startDate,
            endDate = endDate
        )
        loadExpenses()
    }

    private fun loadExpenses() {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Please log in to view expenses"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val state = _uiState.value
                android.util.Log.d("ExpenseHistoryViewModel", "Loading expenses from ${state.startDate} to ${state.endDate}")

                val expenses = expenseRepository.getExpensesBetweenDates(
                    userId = userId,
                    startDate = state.startDate,
                    endDate = state.endDate
                )

                // Convert Firebase expenses to UI model
                val expenseUiList = expenses.map { firebaseExpense ->
                    ExpenseItemUi(
                        id = firebaseExpense.id,
                        amount = firebaseExpense.amount.toDouble(),
                        category = firebaseExpense.categoryName,
                        description = firebaseExpense.notes ?: "",
                        date = firebaseExpense.date.toDate(),
                        receiptPath = firebaseExpense.receiptUrl
                    )
                }.sortedByDescending { it.date } // Sort by date, newest first

                android.util.Log.d("ExpenseHistoryViewModel", "Loaded ${expenseUiList.size} expenses")

                _uiState.value = _uiState.value.copy(
                    expenses = expenseUiList,
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                android.util.Log.e("ExpenseHistoryViewModel", "Error loading expenses", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load expenses"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshExpenses() {
        loadExpenses()
    }
} 