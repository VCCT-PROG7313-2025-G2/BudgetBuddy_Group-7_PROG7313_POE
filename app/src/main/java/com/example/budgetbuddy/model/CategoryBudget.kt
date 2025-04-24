package com.example.budgetbuddy.model

import androidx.annotation.DrawableRes

data class CategoryBudget(
    val categoryId: String, // Unique ID for the category
    val categoryName: String,
    @DrawableRes val categoryIconRes: Int,
    var budgetLimit: Double? // Nullable if no specific limit is set for this category
) 