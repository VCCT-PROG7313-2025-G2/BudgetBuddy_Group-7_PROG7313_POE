package com.example.budgetbuddy.data.db.pojo

import androidx.room.ColumnInfo
import androidx.room.TypeConverters
import com.example.budgetbuddy.data.db.converter.BigDecimalConverter
import java.math.BigDecimal

// POJO to hold the result of the category spending query
@TypeConverters(BigDecimalConverter::class) // Ensure BigDecimal is converted
data class CategorySpending(
    @ColumnInfo(name = "categoryName") val categoryName: String,
    @ColumnInfo(name = "total") val total: BigDecimal
) 