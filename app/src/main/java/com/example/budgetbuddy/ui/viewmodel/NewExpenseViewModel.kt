package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.db.entity.ExpenseEntity
import com.example.budgetbuddy.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

sealed class NewExpenseUiState {
    object Idle : NewExpenseUiState()
    object Loading : NewExpenseUiState()
    object Success : NewExpenseUiState()
    data class Error(val message: String) : NewExpenseUiState()
}

@HiltViewModel
class NewExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
    // TODO: Inject UserRepository if needed to get current user ID
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewExpenseUiState>(NewExpenseUiState.Idle)
    val uiState: StateFlow<NewExpenseUiState> = _uiState.asStateFlow()

    // TODO: Get currentUserId properly
    private val currentUserId: Long = 1L // Placeholder

    fun saveExpense(
        amount: BigDecimal,
        category: String,
        date: Date,
        notes: String?,
        receiptPath: String? // Optional path to receipt image
    ) {
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
                _uiState.value = NewExpenseUiState.Success
            } catch (e: Exception) {
                 _uiState.value = NewExpenseUiState.Error(e.message ?: "Failed to save expense")
            }
        }
    }

     fun resetState() {
        _uiState.value = NewExpenseUiState.Idle
    }
} 