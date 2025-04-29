package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.db.dao.BudgetDao
import com.example.budgetbuddy.data.db.dao.CategoryBudgetDao
import com.example.budgetbuddy.data.db.entity.BudgetEntity
import com.example.budgetbuddy.data.db.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryBudgetDao: CategoryBudgetDao
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

    // Add methods for updating, deleting budgets if needed
} 