package com.example.budgetbuddy.data.firebase.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.math.BigDecimal
import java.util.Date

/**
 * Firebase expense data model for Firestore storage.
 * This replaces the Room ExpenseEntity for cloud storage.
 */
data class FirebaseExpense(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val date: Timestamp = Timestamp.now(),
    val amount: Double = 0.0, // Using Double for Firestore compatibility
    val categoryName: String = "",
    val notes: String? = null,
    val receiptUrl: String? = null, // Firebase Storage URL instead of local path
    val createdAt: Timestamp = Timestamp.now()
) {
    // No-argument constructor required for Firestore
    constructor() : this("")
    
    // Helper methods for BigDecimal and Date conversion
    @Exclude
    fun getAmountAsBigDecimal(): BigDecimal = BigDecimal.valueOf(amount)
    @Exclude
    fun getDateAsDate(): Date = date.toDate()
    
    companion object {
        fun fromBigDecimalAndDate(
            userId: String,
            date: Date,
            amount: BigDecimal,
            categoryName: String,
            notes: String? = null,
            receiptUrl: String? = null
        ): FirebaseExpense {
            return FirebaseExpense(
                userId = userId,
                date = Timestamp(date),
                amount = amount.toDouble(),
                categoryName = categoryName,
                notes = notes,
                receiptUrl = receiptUrl
            )
        }
    }
} 