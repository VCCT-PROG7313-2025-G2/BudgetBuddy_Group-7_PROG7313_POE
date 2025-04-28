package com.example.budgetbuddy.data.db.dao

import androidx.room.*
import com.example.budgetbuddy.data.db.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudget(categoryBudget: CategoryBudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategoryBudgets(categoryBudgets: List<CategoryBudgetEntity>)

    @Update
    suspend fun updateCategoryBudget(categoryBudget: CategoryBudgetEntity)

    @Query("SELECT * FROM category_budgets WHERE budgetId = :budgetId")
    fun getCategoryBudgetsForBudgetId(budgetId: Long): Flow<List<CategoryBudgetEntity>>

    @Query("DELETE FROM category_budgets WHERE budgetId = :budgetId")
    suspend fun deleteCategoryBudgetsForBudgetId(budgetId: Long)
} 