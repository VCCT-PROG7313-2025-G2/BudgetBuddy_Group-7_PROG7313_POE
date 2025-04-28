package com.example.budgetbuddy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.budgetbuddy.data.db.converter.BigDecimalConverter
import com.example.budgetbuddy.data.db.converter.DateConverter
import com.example.budgetbuddy.data.db.dao.*
import com.example.budgetbuddy.data.db.entity.*

@Database(
    entities = [
        UserEntity::class,
        BudgetEntity::class,
        CategoryBudgetEntity::class,
        ExpenseEntity::class,
        RewardPointsEntity::class,
        AchievementEntity::class
    ],
    version = 1, // Start with version 1. Increment if schema changes.
    exportSchema = false // Set to true if you want to export schema for testing/migrations
)
@TypeConverters(DateConverter::class, BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun rewardPointsDao(): RewardPointsDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_buddy_database"
                )
                // Add migrations here if needed later
                // .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration() // Use this only during development!
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 