package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.db.entity.BudgetEntity
import com.example.budgetbuddy.data.db.entity.CategoryBudgetEntity
import com.example.budgetbuddy.data.repository.BudgetRepository
import com.example.budgetbuddy.data.repository.RewardsRepository
import com.example.budgetbuddy.util.Constants
import com.example.budgetbuddy.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class BudgetSetupUiState {
    object Idle : BudgetSetupUiState()
    object Loading : BudgetSetupUiState()
    object Success : BudgetSetupUiState()
    data class Error(val message: String) : BudgetSetupUiState()
}

// Represents a category budget item in the UI list
data class CategoryBudgetInput(
    val categoryName: String,
    var limit: BigDecimal = BigDecimal.ZERO
    // Add iconResId if needed for display
)

@HiltViewModel
class BudgetSetupViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val rewardsRepository: RewardsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<BudgetSetupUiState>(BudgetSetupUiState.Idle)
    val uiState: StateFlow<BudgetSetupUiState> = _uiState.asStateFlow()

    // Get the current user ID from SessionManager
    private fun getCurrentUserId(): Long = sessionManager.getUserId()

    fun saveBudget(totalBudget: BigDecimal, categoryBudgets: List<CategoryBudgetInput>) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == SessionManager.NO_USER_LOGGED_IN) {
             _uiState.value = BudgetSetupUiState.Error("No user logged in.")
             return
        }

        viewModelScope.launch {
            _uiState.value = BudgetSetupUiState.Loading
            try {
                // Get current month/year string
                val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthYear = monthYearFormat.format(Date())

                // Create Budget Entity
                // TODO: If editing, we need to fetch the existing budgetId first
                val budgetEntity = BudgetEntity(
                    userId = currentUserId,
                    monthYear = currentMonthYear,
                    totalAmount = totalBudget
                    // If editing, set budgetId here
                )

                // Create Category Budget Entities (only those with limits > 0)
                val categoryEntities = categoryBudgets
                    .filter { it.limit > BigDecimal.ZERO }
                    .map {
                        CategoryBudgetEntity(
                            // budgetId will be set in repository after main budget insert
                            budgetId = 0, // Placeholder, repository handles linking
                            categoryName = it.categoryName,
                            allocatedAmount = it.limit
                        )
                    }

                budgetRepository.saveBudget(budgetEntity, categoryEntities)

                // Check for first budget set achievement
                rewardsRepository.checkAndUnlockAchievement(currentUserId, Constants.Achievements.FIRST_BUDGET_SET_ID)

                _uiState.value = BudgetSetupUiState.Success

            } catch (e: Exception) {
                _uiState.value = BudgetSetupUiState.Error(e.message ?: "Failed to save budget")
            }
        }
    }

    fun resetState() {
        _uiState.value = BudgetSetupUiState.Idle
    }
} 