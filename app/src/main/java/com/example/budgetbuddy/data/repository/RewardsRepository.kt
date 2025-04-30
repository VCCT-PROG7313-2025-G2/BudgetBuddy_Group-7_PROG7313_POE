package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.db.dao.AchievementDao
import com.example.budgetbuddy.data.db.dao.RewardPointsDao
import com.example.budgetbuddy.data.db.entity.AchievementEntity
import com.example.budgetbuddy.data.db.entity.RewardPointsEntity
import com.example.budgetbuddy.model.UserWithPoints
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.budgetbuddy.util.Constants
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class RewardsRepository @Inject constructor(
    private val rewardPointsDao: RewardPointsDao,
    private val achievementDao: AchievementDao
) {

    // Use Flow version for observation
    fun getPoints(userId: Long): Flow<RewardPointsEntity?> = rewardPointsDao.getPointsForUserFlow(userId)

    fun getAchievements(userId: Long): Flow<List<AchievementEntity>> = achievementDao.getAllAchievementsForUser(userId)

    // Use the robust upsert logic from DAO
    suspend fun addPoints(userId: Long, pointsToAdd: Int) {
        rewardPointsDao.addOrUpdatePoints(userId, pointsToAdd)
        // TODO: Check if adding points unlocks any achievements based on new total
        // val newTotal = rewardPointsDao.getPointsForUserOnce(userId)?.currentPoints ?: 0
        // checkAndUnlockPointAchievements(userId, newTotal)
    }

    suspend fun checkAndUnlockAchievement(userId: Long, achievementId: Long) {
        try {
            val achievement = achievementDao.getAchievementByIdAndUser(achievementId, userId)
            // Check if achievement exists for user AND is not already achieved
            if (achievement != null && achievement.achievedDate == null) {
                unlockAchievement(achievementId)
                // Optional: Could return Boolean indicating if it was newly unlocked
            }
        } catch (e: Exception) {
            // Log error, don't crash the primary operation (saving expense/budget)
             Log.e("RewardsRepository", "Failed to check/unlock achievement $achievementId for user $userId", e)
        }
    }

    // Make private if only used internally
    private suspend fun unlockAchievement(achievementId: Long) {
        achievementDao.markAchievementAchieved(achievementId, Date().time)
        Log.d("RewardsRepository", "Unlocked achievement $achievementId")
    }

    // Method to potentially check and insert initial achievements for a new user
    suspend fun initializeAchievementsForUser(userId: Long) {
        // Check if user already has *any* achievements (simple check)
        val existingAchievements = achievementDao.getAllAchievementsForUser(userId).firstOrNull()
        if (existingAchievements.isNullOrEmpty()) {
            Log.d("RewardsRepository", "Initializing achievements for user $userId")
            val achievementsToSeed = Constants.Achievements.INITIAL_ACHIEVEMENTS.map { seedData ->
                AchievementEntity(
                    achievementId = seedData.id, // Assume ID is defined in Constants
                    userId = userId,
                    achievementName = seedData.name,
                    description = seedData.description,
                    iconName = seedData.iconName,
                    achievedDate = null // Start as locked
                )
            }
            achievementDao.insertAll(achievementsToSeed)
        } else {
             Log.d("RewardsRepository", "Achievements already initialized for user $userId")
        }
         // Also ensure the user has an entry in reward_points table
        rewardPointsDao.insertInitialPoints(RewardPointsEntity(userId = userId, currentPoints = 0))
    }

    // Get Leaderboard Data
    fun getLeaderboard(limit: Int = 10): Flow<List<UserWithPoints>> {
        return rewardPointsDao.getLeaderboard(limit)
    }
} 