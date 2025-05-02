package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.db.entity.BudgetEntity
import com.example.budgetbuddy.data.repository.BudgetRepository
import com.example.budgetbuddy.data.repository.ExpenseRepository
import com.example.budgetbuddy.data.repository.UserRepository
import com.example.budgetbuddy.util.SessionManager
import com.example.budgetbuddy.data.repository.RewardsRepository
import com.example.budgetbuddy.model.UserWithPoints
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat // For currency formatting
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.util.Log

// Data class holding all the information needed for the Profile screen UI.
data class ProfileUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val userEmail: String = "",
    val profileImageUrl: String? = null, // URL of the user's profile image (if any).
    // Text displaying the total budget limit for the current month.
    val budgetLimitText: String = "No Budget Set",
    // Text displaying the remaining budget amount.
    val budgetRemainingText: String = "",
    // Budget progress percentage (0-100) for the progress bar.
    val budgetProgress: Int = 0,
    val error: String? = null
)

// Marks this ViewModel for Hilt injection.
@HiltViewModel
class ProfileViewModel @Inject constructor(
    // Inject necessary repositories and the session manager.
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val rewardsRepository: RewardsRepository, // Keep for potential future use (e.g., showing points).
    private val sessionManager: SessionManager
) : ViewModel() {

    // --- UI State --- 
    // Holds the current state of the Profile screen.
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // --- Initialization --- 
    // Load profile data when the ViewModel is created.
    init {
        loadProfileData()
    }

    // --- Data Loading --- 
    // Fetches user info, budget, and spending data to populate the UI state.
    fun loadProfileData() {
        val userId = sessionManager.getUserId()
        // Stop if no user is logged in.
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
            return
        }

        // Set loading state and launch a coroutine for background work.
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // Determine date range for current month's budget/spending.
                val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthYear = monthYearFormat.format(Date())
                val startOfMonth = getStartOfMonth() // Use helper
                val endOfMonth = getEndOfMonth()     // Use helper

                // Define flows to get user, budget, and spending data.
                val userFlow = userRepository.getUser(userId)
                val budgetFlow = budgetRepository.getBudgetForMonth(userId, currentMonthYear)
                val spentFlow = expenseRepository.getTotalSpendingBetween(userId, startOfMonth, endOfMonth)

                // Combine the latest values from all flows.
                combine(
                    userFlow,
                    budgetFlow,
                    spentFlow
                ) { user, budget, spentAmount ->
                    Log.d("ProfileViewModel", "Combining flows for Profile User ID: $userId")

                    // --- Process Data --- 
                    val name = user?.name ?: "User"
                    val email = user?.email ?: ""
                    val totalBudget = budget?.totalAmount ?: BigDecimal.ZERO
                    val totalSpent = spentAmount ?: BigDecimal.ZERO
                    val remaining = totalBudget - totalSpent

                    // Calculate budget progress percentage.
                    val progress = if (totalBudget > BigDecimal.ZERO) {
                        (totalSpent.divide(totalBudget, 2, BigDecimal.ROUND_HALF_UP) * BigDecimal(100)).toInt().coerceIn(0, 100)
                    } else 0

                    // Format text for budget limit and remaining amount.
                    val limitText = if (budget != null) "${formatCurrency(totalBudget)} Limit" else "No Budget Set"
                    val remainingText = if (budget != null) "${formatCurrency(remaining)} remaining" else ""

                    // --- Create New UI State --- 
                    ProfileUiState(
                        isLoading = false,
                        userName = name,
                        userEmail = email,
                        profileImageUrl = null,
                        budgetLimitText = limitText,
                        budgetRemainingText = remainingText,
                        budgetProgress = progress,
                        error = null // Clear any previous error.
                    )
                }.catch { e -> // Handle errors during the combine operation.
                    Log.e("ProfileViewModel", "Error loading profile data for user $userId", e)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load data.") }
                }.collect { newState -> // Collect the resulting UI state.
                    _uiState.value = newState // Update the state flow.
                }

            } catch (e: Exception) { // Catch other potential errors.
                Log.e("ProfileViewModel", "Exception in loadProfileData", e)
                _uiState.update { it.copy(isLoading = false, error = "An unexpected error occurred.") }
            }
        }
    }

    // --- Helper Functions --- 
    // Formats a BigDecimal amount into a currency string (e.g., "$123.45").
    private fun formatCurrency(amount: BigDecimal): String {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
    }

    // Gets the start date (00:00:00) of the current month.
    private fun getStartOfMonth(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
    }

    // Gets the end date (23:59:59) of the current month.
    private fun getEndOfMonth(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.time
    }
    // --- End Helper Functions --- 
} 