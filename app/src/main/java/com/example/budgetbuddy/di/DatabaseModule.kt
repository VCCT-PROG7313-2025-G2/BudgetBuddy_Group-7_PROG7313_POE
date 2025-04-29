package com.example.budgetbuddy.di

import android.content.Context
import androidx.room.Room
import com.example.budgetbuddy.data.db.AppDatabase
import com.example.budgetbuddy.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "budget_buddy_database"
        )
        .fallbackToDestructiveMigration() // Consider proper migrations for production
        .build()
    }

    @Provides
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    fun provideBudgetDao(appDatabase: AppDatabase): BudgetDao {
        return appDatabase.budgetDao()
    }

    @Provides
    fun provideCategoryBudgetDao(appDatabase: AppDatabase): CategoryBudgetDao {
        return appDatabase.categoryBudgetDao()
    }

    @Provides
    fun provideExpenseDao(appDatabase: AppDatabase): ExpenseDao {
        return appDatabase.expenseDao()
    }

    @Provides
    fun provideRewardPointsDao(appDatabase: AppDatabase): RewardPointsDao {
        return appDatabase.rewardPointsDao()
    }

    @Provides
    fun provideAchievementDao(appDatabase: AppDatabase): AchievementDao {
        return appDatabase.achievementDao()
    }
} 