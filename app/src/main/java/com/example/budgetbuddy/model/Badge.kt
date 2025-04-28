package com.example.budgetbuddy.model

import androidx.annotation.DrawableRes

data class Badge(
    val id: String,
    val name: String,
    @DrawableRes val iconResId: Int
    // Add description or other fields if needed
) 