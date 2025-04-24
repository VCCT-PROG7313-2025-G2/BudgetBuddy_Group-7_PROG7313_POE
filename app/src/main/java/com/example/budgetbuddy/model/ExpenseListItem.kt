package com.example.budgetbuddy.model

import androidx.annotation.DrawableRes
import java.util.Date

// Use a sealed class to represent different types of items in the RecyclerView
sealed class ExpenseListItem {
    abstract val id: String // Unique ID for DiffUtil

    data class Expense(
        val expenseId: Long,
        val categoryName: String,
        val notes: String?,
        val amount: Double,
        @DrawableRes val categoryIconRes: Int, // Placeholder for icon resource
        val date: Date,
        val hasReceipt: Boolean,
        override val id: String = "expense_$expenseId"
    ) : ExpenseListItem()

    data class DateHeader(
        val dateString: String,
        override val id: String = "header_$dateString"
    ) : ExpenseListItem()
} 