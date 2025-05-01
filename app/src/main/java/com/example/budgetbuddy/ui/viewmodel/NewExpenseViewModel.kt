package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.db.entity.ExpenseEntity
import com.example.budgetbuddy.data.repository.ExpenseRepository
import com.example.budgetbuddy.data.repository.RewardsRepository
import com.example.budgetbuddy.data.repository.BudgetRepository
import com.example.budgetbuddy.util.Constants
import com.example.budgetbuddy.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Date
import java.util.Locale
import java.util.Calendar
import javax.inject.Inject
import android.net.Uri
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

sealed class NewExpenseUiState {
    object Idle : NewExpenseUiState()
    object Loading : NewExpenseUiState()
    object Success : NewExpenseUiState()
    data class Error(val message: String) : NewExpenseUiState()
}

@HiltViewModel
class NewExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val rewardsRepository: RewardsRepository,
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewExpenseUiState>(NewExpenseUiState.Idle)
    val uiState: StateFlow<NewExpenseUiState> = _uiState.asStateFlow()

    private val userId = sessionManager.getUserId()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<String>> = budgetRepository.getRelevantCategoryNamesForPeriod(
        userId,
        getStartOfMonth(),
        getEndOfMonth()
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun getCurrentUserId(): Long = sessionManager.getUserId()

    fun saveExpense(
        amount: BigDecimal,
        category: String,
        date: Date,
        notes: String?,
        receiptPath: String?
    ) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == SessionManager.NO_USER_LOGGED_IN) {
            _uiState.value = NewExpenseUiState.Error("No user logged in.")
            return
        }

        viewModelScope.launch {
            _uiState.value = NewExpenseUiState.Loading
            if (amount <= BigDecimal.ZERO) {
                _uiState.value = NewExpenseUiState.Error("Amount must be positive")
                return@launch
            }
            if (category.isBlank()) {
                _uiState.value = NewExpenseUiState.Error("Category cannot be empty")
                return@launch
            }

            try {
                val expense = ExpenseEntity(
                    userId = currentUserId,
                    amount = amount,
                    categoryName = category,
                    date = date,
                    notes = notes?.takeIf { it.isNotBlank() },
                    receiptPath = receiptPath
                )
                expenseRepository.insertExpense(expense)

                // Award points for logging expense
                rewardsRepository.addPoints(currentUserId, 10)

                // Check for first expense achievement
                rewardsRepository.checkAndUnlockAchievement(currentUserId, Constants.Achievements.FIRST_EXPENSE_LOGGED_ID)

                _uiState.value = NewExpenseUiState.Success
            } catch (e: Exception) {
                 _uiState.value = NewExpenseUiState.Error(e.message ?: "Failed to save expense")
            }
        }
    }

     fun resetState() {
        _uiState.value = NewExpenseUiState.Idle
    }

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

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }
} 