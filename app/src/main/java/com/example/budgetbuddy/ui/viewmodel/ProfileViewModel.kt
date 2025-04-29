package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.db.entity.BudgetEntity
import com.example.budgetbuddy.data.repository.BudgetRepository
import com.example.budgetbuddy.data.repository.ExpenseRepository
import com.example.budgetbuddy.data.repository.UserRepository
import com.example.budgetbuddy.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat // For currency formatting
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.util.Log

data class ProfileUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val userEmail: String = "",
    val profileImageUrl: String? = null, // Placeholder for future use
    val budgetLimitText: String = "No Budget Set",
    val budgetRemainingText: String = "",
    val budgetProgress: Int = 0, // 0-100
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        val userId = sessionManager.getUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // Get current month for budget/spending
                val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthYear = monthYearFormat.format(Date())
                val startOfMonth = getStartOfMonth().time
                val endOfMonth = getEndOfMonth().time

                combine(
                    userRepository.getUser(userId),
                    budgetRepository.getBudgetForMonth(userId, currentMonthYear),
                    expenseRepository.getTotalSpendingBetween(userId, Date(startOfMonth), Date(endOfMonth))
                ) { user, budget, spentAmount ->

                    val name = user?.name ?: "User"
                    val email = user?.email ?: ""
                    val totalBudget = budget?.totalAmount ?: BigDecimal.ZERO
                    val totalSpent = spentAmount ?: BigDecimal.ZERO
                    val remaining = totalBudget - totalSpent

                    val progress = if (totalBudget > BigDecimal.ZERO) {
                        (totalSpent.divide(totalBudget, 2, BigDecimal.ROUND_HALF_UP) * BigDecimal(100)).toInt().coerceIn(0, 100)
                    } else 0

                    val limitText = if (budget != null) "${formatCurrency(totalBudget)} Limit" else "No Budget Set"
                    val remainingText = if (budget != null) "${formatCurrency(remaining)} remaining" else ""

                    ProfileUiState(
                        isLoading = false,
                        userName = name,
                        userEmail = email,
                        budgetLimitText = limitText,
                        budgetRemainingText = remainingText,
                        budgetProgress = progress,
                        error = null
                    )
                }.catch { e ->
                    Log.e("ProfileViewModel", "Error loading profile data for user $userId", e)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load data.") }
                }.collect { newState ->
                    _uiState.value = newState
                }

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception in loadProfileData", e)
                _uiState.update { it.copy(isLoading = false, error = "An unexpected error occurred.") }
            }
        }
    }

    // --- Helper Functions --- (Could move to a Util class)
    private fun formatCurrency(amount: BigDecimal): String {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount) // Adjust Locale if needed
    }

    private fun getStartOfMonth(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun getEndOfMonth(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.time
    }
} 