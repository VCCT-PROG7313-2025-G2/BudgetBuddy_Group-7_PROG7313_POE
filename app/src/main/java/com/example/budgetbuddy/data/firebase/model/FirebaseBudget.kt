package com.example.budgetbuddy.data.firebase.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.math.BigDecimal

/**
 * Firebase budget data model for Firestore storage.
 * This replaces the Room BudgetEntity for cloud storage.
 */
data class FirebaseBudget(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val monthYear: String = "", // Format: "yyyy-MM"
    val totalAmount: Double = 0.0, // Using Double for Firestore compatibility
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    // No-argument constructor required for Firestore
    constructor() : this("")
    
    // Helper methods for BigDecimal conversion
    @Exclude
    fun getTotalAmountAsBigDecimal(): BigDecimal = BigDecimal.valueOf(totalAmount)
    
    companion object {
        fun fromBigDecimal(
            userId: String,
            monthYear: String,
            totalAmount: BigDecimal
        ): FirebaseBudget {
            return FirebaseBudget(
                userId = userId,
                monthYear = monthYear,
                totalAmount = totalAmount.toDouble()
            )
        }
    }
} 