package com.example.budgetbuddy.model

import androidx.annotation.DrawableRes

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    @DrawableRes val iconRes: Int, // Icon for locked/unlocked state
    val isUnlocked: Boolean,
    val progress: Int? = null, // Optional: Current progress (e.g., 5)
    val progressMax: Int? = null // Optional: Max progress for completion (e.g., 10)
) 