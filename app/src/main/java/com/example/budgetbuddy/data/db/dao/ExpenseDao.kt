package com.example.budgetbuddy.data.db.dao

import androidx.room.*
import com.example.budgetbuddy.data.db.entity.ExpenseEntity
import com.example.budgetbuddy.data.db.pojo.CategorySpending
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    fun getAllExpensesForUser(userId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesForUserBetweenDates(userId: Long, startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    fun getTotalSpendingBetweenDates(userId: Long, startDate: Long, endDate: Long): Flow<BigDecimal?>

    @Query("SELECT categoryName, SUM(amount) as total FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate GROUP BY categoryName")
    fun getSpendingByCategoryBetweenDates(userId: Long, startDate: Long, endDate: Long): Flow<List<CategorySpending>>

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY amount DESC LIMIT 1")
    fun getBiggestExpense(userId: Long): Flow<ExpenseEntity?>
} 