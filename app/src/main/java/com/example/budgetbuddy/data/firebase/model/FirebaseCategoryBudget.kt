package com.example.budgetbuddy.data.firebase.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.math.BigDecimal

/**
 * Firebase category budget data model for Firestore storage.
 * This replaces the Room CategoryBudgetEntity for cloud storage.
 */
data class FirebaseCategoryBudget(
    @DocumentId
    val id: String = "",
    val budgetId: String = "", // Reference to parent budget
    val categoryName: String = "",
    val allocatedAmount: Double = 0.0, // Using Double for Firestore compatibility
    val createdAt: Timestamp = Timestamp.now()
) {
    // No-argument constructor required for Firestore
    constructor() : this("")
    
    // Helper methods for BigDecimal conversion
    @Exclude
    fun getAllocatedAmountAsBigDecimal(): BigDecimal = BigDecimal.valueOf(allocatedAmount)
    
    companion object {
        fun fromBigDecimal(
            budgetId: String,
            categoryName: String,
            allocatedAmount: BigDecimal
        ): FirebaseCategoryBudget {
            return FirebaseCategoryBudget(
                budgetId = budgetId,
                categoryName = categoryName,
                allocatedAmount = allocatedAmount.toDouble()
            )
        }
    }
} 