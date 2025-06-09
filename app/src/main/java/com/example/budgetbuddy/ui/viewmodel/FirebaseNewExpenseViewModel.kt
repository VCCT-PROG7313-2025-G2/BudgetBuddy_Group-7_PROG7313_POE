package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.firebase.repository.FirebaseExpenseRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseRewardsRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseBudgetRepository
import com.example.budgetbuddy.util.Constants
import com.example.budgetbuddy.util.FirebaseSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Date
import java.util.Calendar
import javax.inject.Inject
import android.net.Uri

/**
 * Firebase-based NewExpenseViewModel that replaces the Room-based version.
 * Uses Firebase repositories for expense management with receipt upload support.
 */

// UI States remain the same for compatibility
sealed class FirebaseNewExpenseUiState {
    object Idle : FirebaseNewExpenseUiState()
    object Loading : FirebaseNewExpenseUiState()
    object Success : FirebaseNewExpenseUiState()
    data class Error(val message: String) : FirebaseNewExpenseUiState()
}

@HiltViewModel
class FirebaseNewExpenseViewModel @Inject constructor(
    private val expenseRepository: FirebaseExpenseRepository,
    private val rewardsRepository: FirebaseRewardsRepository,
    private val budgetRepository: FirebaseBudgetRepository,
    private val sessionManager: FirebaseSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<FirebaseNewExpenseUiState>(FirebaseNewExpenseUiState.Idle)
    val uiState: StateFlow<FirebaseNewExpenseUiState> = _uiState.asStateFlow()

    // Get the current user's ID
    private val userId = sessionManager.getUserId()

    // Dynamic Categories
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Note: categories property is defined later in the class

    /**
     * Saves a new expense with optional receipt upload.
     */
    fun saveExpense(
        amount: BigDecimal,
        category: String,
        date: Date,
        notes: String?,
        receiptUri: Uri? = null
    ) {
        val currentUserId = getCurrentUserId()
        if (currentUserId.isEmpty()) {
            _uiState.value = FirebaseNewExpenseUiState.Error("No user logged in.")
            return
        }

        viewModelScope.launch {
            _uiState.value = FirebaseNewExpenseUiState.Loading
            
            // Basic validation
            if (amount <= BigDecimal.ZERO) {
                _uiState.value = FirebaseNewExpenseUiState.Error("Amount must be positive")
                return@launch
            }
            if (category.isBlank()) {
                _uiState.value = FirebaseNewExpenseUiState.Error("Category cannot be empty")
                return@launch
            }

            try {
                // Insert expense (repository handles receipt upload if provided)
                val result = expenseRepository.insertExpense(
                    userId = currentUserId,
                    amount = amount,
                    categoryName = category,
                    date = date,
                    notes = notes?.takeIf { it.isNotBlank() },
                    receiptUri = receiptUri
                )

                result.onSuccess { expenseId ->
                    // Award points for logging the expense
                    rewardsRepository.addPoints(currentUserId, 10)
                    
                    // Check if the user unlocked the "First Expense" achievement
                    rewardsRepository.checkAndUnlockAchievement(
                        currentUserId, 
                        Constants.Achievements.FIRST_EXPENSE_LOGGED_FIREBASE_ID
                    )

                    _uiState.value = FirebaseNewExpenseUiState.Success
                }.onFailure { exception ->
                    _uiState.value = FirebaseNewExpenseUiState.Error(
                        exception.message ?: "Failed to save expense"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = FirebaseNewExpenseUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Updates an existing expense.
     */
    fun updateExpense(
        expenseId: String,
        amount: BigDecimal,
        category: String,
        date: Date,
        notes: String?,
        newReceiptUri: Uri? = null
    ) {
        val currentUserId = getCurrentUserId()
        if (currentUserId.isEmpty()) {
            _uiState.value = FirebaseNewExpenseUiState.Error("No user logged in.")
            return
        }

        viewModelScope.launch {
            _uiState.value = FirebaseNewExpenseUiState.Loading
            
            // Basic validation
            if (amount <= BigDecimal.ZERO) {
                _uiState.value = FirebaseNewExpenseUiState.Error("Amount must be positive")
                return@launch
            }
            if (category.isBlank()) {
                _uiState.value = FirebaseNewExpenseUiState.Error("Category cannot be empty")
                return@launch
            }

            try {
                val result = expenseRepository.updateExpense(
                    expenseId = expenseId,
                    amount = amount,
                    categoryName = category,
                    date = date,
                    notes = notes?.takeIf { it.isNotBlank() },
                    newReceiptUri = newReceiptUri
                )

                result.onSuccess {
                    _uiState.value = FirebaseNewExpenseUiState.Success
                }.onFailure { exception ->
                    _uiState.value = FirebaseNewExpenseUiState.Error(
                        exception.message ?: "Failed to update expense"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = FirebaseNewExpenseUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Deletes an expense.
     */
    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _uiState.value = FirebaseNewExpenseUiState.Loading
            
            try {
                val result = expenseRepository.deleteExpense(expenseId)
                
                result.onSuccess {
                    _uiState.value = FirebaseNewExpenseUiState.Success
                }.onFailure { exception ->
                    _uiState.value = FirebaseNewExpenseUiState.Error(
                        exception.message ?: "Failed to delete expense"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = FirebaseNewExpenseUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Sets the selected category.
     */
    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    /**
     * Resets the UI state to Idle.
     */
    fun resetState() {
        _uiState.value = FirebaseNewExpenseUiState.Idle
    }

    /**
     * Gets available categories from current budget for expense categorization.
     */
    val categories: StateFlow<List<String>> = budgetRepository.getCurrentBudgetCategories(getCurrentUserId())
        .map { categories -> categories.map { it.categoryName } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Gets the current user ID from Firebase session manager.
     */
    private fun getCurrentUserId(): String = sessionManager.getUserId()

    /**
     * Helper function to get the start of the current month.
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

    /**
     * Helper function to get the end of the current month.
     */
    private fun getEndOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    /**
     * Validates if the current user is logged in.
     */
    fun isUserLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    /**
     * Gets available categories for the current period.
     */
    suspend fun getAvailableCategories(): List<String> {
        return budgetRepository.getRelevantCategoryNamesForPeriod(
            userId, 
            getStartOfMonth(), 
            getEndOfMonth()
        )
    }
} 