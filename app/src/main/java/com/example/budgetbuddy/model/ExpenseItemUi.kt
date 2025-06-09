package com.example.budgetbuddy.model

import java.math.BigDecimal
import java.util.Date


//Data class representing an expense item specifically for UI display,
 // potentially different from the database entity.

data class ExpenseItemUi(
    val id: String,
    val amount: Double,
    val category: String,
    val date: Date,
    val description: String,
    val receiptPath: String? // Path to the locally stored receipt image or URL
) 