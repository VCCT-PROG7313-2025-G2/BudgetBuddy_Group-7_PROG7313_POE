package com.example.budgetbuddy.model

import androidx.annotation.DrawableRes
import androidx.room.Embedded
import com.example.budgetbuddy.data.db.entity.RewardPointsEntity
import com.example.budgetbuddy.data.db.entity.UserEntity

/**
 * Pojo class to combine User and their Reward Points for leaderboard display.
 */
data class UserWithPoints(
    @Embedded val user: UserEntity,
    @Embedded(prefix = "points_") val points: RewardPointsEntity
    // Add other fields if needed directly from queries
) 