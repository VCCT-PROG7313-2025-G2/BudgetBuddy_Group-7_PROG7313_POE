package com.example.budgetbuddy.data.db.dao

import androidx.room.*
import com.example.budgetbuddy.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear LIMIT 1")
    fun getBudgetForMonth(userId: Long, monthYear: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE budgetId = :budgetId LIMIT 1")
    suspend fun getBudgetById(budgetId: Long): BudgetEntity?

    @Query("SELECT SUM(totalAmount) FROM budgets WHERE userId = :userId")
    fun getTotalBudgetAllocationForUser(userId: Long): Flow<BigDecimal?>
} 