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

// Defines the possible states for the New Expense UI.
sealed class NewExpenseUiState {
    // Initial state.
    object Idle : NewExpenseUiState()
    // Saving is in progress.
    object Loading : NewExpenseUiState()
    // Saving was successful.
    object Success : NewExpenseUiState()
    // An error occurred during saving.
    data class Error(val message: String) : NewExpenseUiState()
}

// Marks this ViewModel for Hilt injection.
@HiltViewModel
class NewExpenseViewModel @Inject constructor(
    // Inject repositories needed for saving and getting data.
    private val expenseRepository: ExpenseRepository,
    private val rewardsRepository: RewardsRepository,
    private val budgetRepository: BudgetRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // --- UI State --- 
    // Holds the current state (Idle, Loading, Success, Error).
    private val _uiState = MutableStateFlow<NewExpenseUiState>(NewExpenseUiState.Idle)
    val uiState: StateFlow<NewExpenseUiState> = _uiState.asStateFlow()

    // Get the current user's ID.
    private val userId = sessionManager.getUserId()

    // --- Dynamic Categories --- 
    // StateFlow to hold the currently selected category (if needed, might be unused).
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Provides the list of relevant category names to the UI.
    // Fetches categories relevant for the current month from the BudgetRepository.
    // `stateIn` converts the Flow to a StateFlow, caching the last value and sharing it among collectors.
    val categories: StateFlow<List<String>> = budgetRepository.getRelevantCategoryNamesForPeriod(
        userId,
        getStartOfMonth(), // Helper function to get the start date of the current month.
        getEndOfMonth()    // Helper function to get the end date of the current month.
    ).stateIn(
        scope = viewModelScope, // Coroutine scope for this ViewModel.
        started = SharingStarted.WhileSubscribed(5000), // Keep the flow active for 5s after last observer disappears.
        initialValue = emptyList() // Start with an empty list until data loads.
    )

    // Helper function to get the current user ID.
    private fun getCurrentUserId(): Long = sessionManager.getUserId()

    // Function called by the Fragment to save a new expense.
    fun saveExpense(
        amount: BigDecimal,
        category: String,
        date: Date,
        notes: String?,
        receiptPath: String? // Path to the copied receipt image, if any.
    ) {
        val currentUserId = getCurrentUserId()
        // Check if a user is logged in.
        if (currentUserId == SessionManager.NO_USER_LOGGED_IN) {
            _uiState.value = NewExpenseUiState.Error("No user logged in.")
            return
        }

        // Launch a coroutine for background work (database operations).
        viewModelScope.launch {
            // Set state to Loading.
            _uiState.value = NewExpenseUiState.Loading
            // Basic validation.
            if (amount <= BigDecimal.ZERO) {
                _uiState.value = NewExpenseUiState.Error("Amount must be positive")
                return@launch
            }
            if (category.isBlank()) {
                _uiState.value = NewExpenseUiState.Error("Category cannot be empty")
                return@launch
            }

            try {
                // Create the ExpenseEntity object with the provided data.
                val expense = ExpenseEntity(
                    userId = currentUserId,
                    amount = amount,
                    categoryName = category,
                    date = date,
                    notes = notes?.takeIf { it.isNotBlank() }, // Save notes only if not blank.
                    receiptPath = receiptPath // Save the path to the receipt image.
                )
                // Insert the expense into the database via the repository.
                expenseRepository.insertExpense(expense)

                // --- Rewards Logic --- 
                // Award points for logging the expense.
                rewardsRepository.addPoints(currentUserId, 10)
                // Check if the user unlocked the "First Expense" achievement.
                rewardsRepository.checkAndUnlockAchievement(currentUserId, Constants.Achievements.FIRST_EXPENSE_LOGGED_ID)

                // Set state to Success if everything worked.
                _uiState.value = NewExpenseUiState.Success
            } catch (e: Exception) {
                // Set state to Error if any exception occurred.
                 _uiState.value = NewExpenseUiState.Error(e.message ?: "Failed to save expense")
            }
        }
    }

    // Resets the UI state back to Idle (usually called after Success/Error is handled).
     fun resetState() {
        _uiState.value = NewExpenseUiState.Idle
    }

    // --- Date Helper Functions (Consider moving to a shared DateUtils object) ---
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
    // --- End Date Helpers ---

    // Function to potentially track the selected category (might be unused).
    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }
}