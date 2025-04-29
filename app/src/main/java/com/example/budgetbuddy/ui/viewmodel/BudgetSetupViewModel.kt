package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.db.entity.BudgetEntity
import com.example.budgetbuddy.data.db.entity.CategoryBudgetEntity
import com.example.budgetbuddy.data.repository.BudgetRepository
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
    private val budgetRepository: BudgetRepository
    // TODO: Inject UserRepository if needed to get current user ID
) : ViewModel() {

    private val _uiState = MutableStateFlow<BudgetSetupUiState>(BudgetSetupUiState.Idle)
    val uiState: StateFlow<BudgetSetupUiState> = _uiState.asStateFlow()

    // TODO: Fetch existing budget/categories to pre-populate if editing
    // TODO: Get currentUserId properly
    private val currentUserId: Long = 1L // Placeholder

    fun saveBudget(totalBudget: BigDecimal, categoryBudgets: List<CategoryBudgetInput>) {
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