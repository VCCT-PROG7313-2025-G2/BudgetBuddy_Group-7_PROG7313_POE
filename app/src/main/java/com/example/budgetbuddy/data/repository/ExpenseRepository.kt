package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.db.dao.ExpenseDao
import com.example.budgetbuddy.data.db.entity.ExpenseEntity
import com.example.budgetbuddy.data.db.pojo.CategorySpending
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {

    suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    fun getAllExpenses(userId: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getAllExpensesForUser(userId)

    fun getExpensesBetween(userId: Long, startDate: Date, endDate: Date): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesForUserBetweenDates(userId, startDate.time, endDate.time)

    fun getTotalSpendingBetween(userId: Long, startDate: Date, endDate: Date): Flow<BigDecimal?> =
        expenseDao.getTotalSpendingBetweenDates(userId, startDate.time, endDate.time)

    fun getSpendingByCategoryBetween(userId: Long, startDate: Date, endDate: Date): Flow<List<CategorySpending>> =
        expenseDao.getSpendingByCategoryBetweenDates(userId, startDate.time, endDate.time)

    fun getBiggestExpenseFlow(userId: Long): Flow<ExpenseEntity?> =
        expenseDao.getBiggestExpense(userId)

    // You might add non-Flow versions for specific calculations
    suspend fun calculateAverageDailySpending(userId: Long, startDate: Date, endDate: Date): BigDecimal {
        // TODO: Implement actual calculation logic
        return BigDecimal("45.67") // Placeholder
    }

     suspend fun findBiggestExpense(userId: Long, startDate: Date, endDate: Date): ExpenseEntity? {
        // TODO: Implement actual query logic (modify DAO or filter here)
        return null // Placeholder
    }
} 