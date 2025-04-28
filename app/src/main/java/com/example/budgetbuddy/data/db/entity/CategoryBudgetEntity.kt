package com.example.budgetbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "category_budgets",
    foreignKeys = [ForeignKey(
        entity = BudgetEntity::class,
        parentColumns = ["budgetId"],
        childColumns = ["budgetId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index(value = ["budgetId", "categoryName"], unique = true)]
)
data class CategoryBudgetEntity(
    @PrimaryKey(autoGenerate = true) val catBudgetId: Long = 0,
    val budgetId: Long, // Foreign key to BudgetEntity
    val categoryName: String,
    val allocatedAmount: BigDecimal
) 