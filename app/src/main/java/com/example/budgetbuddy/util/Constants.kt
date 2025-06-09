package com.example.budgetbuddy.util

import javax.inject.Singleton
import java.math.BigDecimal

// Helper data class for seeding
data class AchievementSeedData(
    val id: Long,
    val name: String,
    val description: String,
    val iconName: String?
)

// Add Constants object if it doesn't exist
object Constants {
    object Budget {
        // Minimum budget amount required for budget setup
        val MINIMUM_BUDGET_AMOUNT: BigDecimal = BigDecimal("50.00")
        // Minimum amount for individual category budgets
        val MINIMUM_CATEGORY_AMOUNT: BigDecimal = BigDecimal("5.00")
    }
    
    object Achievements {
        // Room-based achievement IDs (Long)
        const val FIRST_EXPENSE_LOGGED_ID: Long = 101
        const val FIRST_BUDGET_SET_ID: Long = 102
        
        // Firebase-based achievement IDs (String)
        const val FIRST_EXPENSE_LOGGED_FIREBASE_ID: String = "first_expense_logged"
        const val BUDGET_CREATED_ID: String = "budget_created"
        const val WEEK_UNDER_BUDGET_ID: String = "week_under_budget"
        const val MONTH_UNDER_BUDGET_ID: String = "month_under_budget"
        const val HUNDRED_POINTS_ID: String = "hundred_points"

        // Define initial achievement data to be seeded
        val INITIAL_ACHIEVEMENTS = listOf(
            AchievementSeedData(FIRST_EXPENSE_LOGGED_ID, "First Expense Logged", "Tracked your first expense.", "ic_track_expenses"),
            AchievementSeedData(FIRST_BUDGET_SET_ID, "First Budget Set", "You successfully set your first monthly budget!", "ic_set_goals")
            // Add other achievements here that every user starts with
        )
    }
} 