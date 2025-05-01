package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.db.dao.BudgetDao
import com.example.budgetbuddy.data.db.dao.CategoryBudgetDao
import com.example.budgetbuddy.data.db.entity.BudgetEntity
import com.example.budgetbuddy.data.db.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import com.example.budgetbuddy.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryBudgetDao: CategoryBudgetDao,
    private val expenseRepository: ExpenseRepository
) {

    fun getBudgetForMonth(userId: Long, monthYear: String): Flow<BudgetEntity?> =
        budgetDao.getBudgetForMonth(userId, monthYear)

    fun getCategoryBudgets(budgetId: Long): Flow<List<CategoryBudgetEntity>> =
        categoryBudgetDao.getCategoryBudgetsForBudgetId(budgetId)

    suspend fun saveBudget(budget: BudgetEntity, categoryBudgets: List<CategoryBudgetEntity>) {
        val budgetId = budgetDao.insertBudget(budget)
        // Ensure category budgets have the correct budgetId before inserting
        val linkedCategoryBudgets = categoryBudgets.map { it.copy(budgetId = budgetId) }
        // Consider deleting old ones first if this is an update
        // categoryBudgetDao.deleteCategoryBudgetsForBudgetId(budgetId)
        categoryBudgetDao.insertAllCategoryBudgets(linkedCategoryBudgets)
    }

    /**
     * Gets a sorted list of unique category names relevant for a given user and period.
     * Relevant means the category either had spending or a budget limit set.
     */
    fun getRelevantCategoryNamesForPeriod(userId: Long, startDate: Date, endDate: Date): Flow<List<String>> {
        // Flow for budget ID for the period
        val budgetIdFlow = getBudgetForMonth(userId, com.example.budgetbuddy.util.DateUtils.formatDateToMonthYear(startDate))
            .map { it?.budgetId }

        // Combine budget ID flow with spending flow
        return combine(budgetIdFlow, expenseRepository.getSpendingByCategoryBetween(userId, startDate, endDate)) { budgetId, spendingList ->
            val spendingCategories = spendingList.map { it.categoryName }.toSet()
            val limitCategories = if (budgetId != null) {
                // Fetch limits only if budget exists, convert to set of names
                categoryBudgetDao.getCategoryBudgetsForBudgetIdOnce(budgetId).map { it.categoryName }.toSet()
            } else {
                emptySet()
            }

            // Combine, sort, and return
            (spendingCategories + limitCategories).sorted()
        }
    }

    // Add methods for updating, deleting budgets if needed
} 