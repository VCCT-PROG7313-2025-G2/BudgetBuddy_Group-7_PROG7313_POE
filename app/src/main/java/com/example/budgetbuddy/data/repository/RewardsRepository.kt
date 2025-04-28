package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.db.dao.AchievementDao
import com.example.budgetbuddy.data.db.dao.RewardPointsDao
import com.example.budgetbuddy.data.db.entity.AchievementEntity
import com.example.budgetbuddy.data.db.entity.RewardPointsEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardsRepository @Inject constructor(
    private val rewardPointsDao: RewardPointsDao,
    private val achievementDao: AchievementDao
) {

    fun getPoints(userId: Long): Flow<RewardPointsEntity?> = rewardPointsDao.getPointsForUser(userId)

    fun getAchievements(userId: Long): Flow<List<AchievementEntity>> = achievementDao.getAllAchievementsForUser(userId)

    suspend fun addPoints(userId: Long, pointsToAdd: Int) {
        rewardPointsDao.addPoints(userId, pointsToAdd)
        // TODO: Check if adding points unlocks any achievements
    }

    suspend fun unlockAchievement(achievementId: Long) {
        achievementDao.markAchievementAchieved(achievementId, Date().time)
    }

    // Method to potentially check and insert initial achievements for a new user
    suspend fun initializeAchievementsForUser(userId: Long) {
        // TODO: Check if achievements already exist for user
        // TODO: Insert default/initial achievements (e.g., "Create first budget", "Add first expense")
        // Example:
        // val initialAchievement = AchievementEntity(userId = userId, achievementName = "Welcome!", ...)
        // achievementDao.insert(initialAchievement)
    }
} 