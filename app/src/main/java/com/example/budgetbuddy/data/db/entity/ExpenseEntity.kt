package com.example.budgetbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.budgetbuddy.data.db.converter.DateConverter
import com.example.budgetbuddy.data.db.converter.BigDecimalConverter
import java.math.BigDecimal
import java.util.Date

@Entity(tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index(value = ["userId", "date"])]
)
@TypeConverters(DateConverter::class, BigDecimalConverter::class)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val expenseId: Long = 0,
    val userId: Long,
    val date: Date,
    val amount: BigDecimal,
    val categoryName: String,
    val notes: String?,
    val receiptPath: String? // Store path to image file if attached
) 