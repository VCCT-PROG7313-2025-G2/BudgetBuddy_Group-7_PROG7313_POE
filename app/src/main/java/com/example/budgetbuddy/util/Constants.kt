package com.example.budgetbuddy.util

import javax.inject.Singleton

// Helper data class for seeding
data class AchievementSeedData(
    val id: Long,
    val name: String,
    val description: String,
    val iconName: String?
)

// Add Constants object if it doesn't exist
object Constants {
    object Achievements {
        const val FIRST_EXPENSE_LOGGED_ID: Long = 101
        const val FIRST_BUDGET_SET_ID: Long = 102
        // Add other achievement/badge IDs here

        // Define initial achievement data to be seeded
        val INITIAL_ACHIEVEMENTS = listOf(
            AchievementSeedData(FIRST_EXPENSE_LOGGED_ID, "First Expense Logged", "Tracked your first expense.", "ic_track_expenses"),
            AchievementSeedData(FIRST_BUDGET_SET_ID, "First Budget Set", "You successfully set your first monthly budget!", "ic_set_goals")
            // Add other achievements here that every user starts with
        )
    }
} 