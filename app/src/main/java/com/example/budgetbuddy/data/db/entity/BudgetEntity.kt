package com.example.budgetbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.math.BigDecimal // Use BigDecimal for currency

@Entity(tableName = "budgets",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index(value = ["userId", "monthYear"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val budgetId: Long = 0,
    val userId: Long,
    val monthYear: String, // Format like "YYYY-MM"
    val totalAmount: BigDecimal // Store currency as BigDecimal
) 