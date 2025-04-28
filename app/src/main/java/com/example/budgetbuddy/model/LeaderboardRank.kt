package com.example.budgetbuddy.model

import androidx.annotation.DrawableRes

data class LeaderboardRank(
    val userId: String,
    val name: String,
    @DrawableRes val profileImageRes: Int, // Or String for URL
    val points: Int,
    // Rank will be determined by position in the list
) 