package com.example.budgetbuddy.data.firebase.repository

import com.example.budgetbuddy.data.firebase.model.FirebaseRewardPoints
import com.example.budgetbuddy.data.firebase.model.FirebaseAchievement
import com.example.budgetbuddy.data.firebase.model.FirebaseUser
import com.example.budgetbuddy.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase rewards repository that handles points and achievements management using Firestore.
 * This replaces the Room-based RewardsRepository.
 */
@Singleton
class FirebaseRewardsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val REWARD_POINTS_COLLECTION = "rewardPoints"
        private const val ACHIEVEMENTS_COLLECTION = "achievements"
        private const val USERS_COLLECTION = "users"
    }

    /**
     * Initializes rewards for a new user.
     * Creates initial reward points and predefined achievements.
     */
    suspend fun initializeRewardsForUser(userId: String): Result<Unit> {
        return try {
            // Create initial reward points
            val initialPoints = FirebaseRewardPoints.create(userId, 0)
            firestore.collection(REWARD_POINTS_COLLECTION)
                .document(userId)
                .set(initialPoints)
                .await()
            
            // Create predefined achievements
            initializePredefinedAchievements(userId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds points to a user's account.
     */
    suspend fun addPoints(userId: String, points: Int): Result<Unit> {
        return try {
            val pointsDocRef = firestore.collection(REWARD_POINTS_COLLECTION).document(userId)
            
            // Use Firestore atomic increment
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(pointsDocRef)
                if (snapshot.exists()) {
                    transaction.update(pointsDocRef, mapOf(
                        "currentPoints" to FieldValue.increment(points.toLong()),
                        "lastUpdated" to com.google.firebase.Timestamp.now()
                    ))
                } else {
                    // Create if doesn't exist
                    val newPoints = FirebaseRewardPoints.create(userId, points)
                    transaction.set(pointsDocRef, newPoints)
                }
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets current points for a user.
     */
    suspend fun getCurrentPoints(userId: String): Int {
        return try {
            val document = firestore.collection(REWARD_POINTS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            document.toObject(FirebaseRewardPoints::class.java)?.currentPoints ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Gets user points as Flow for real-time updates.
     */
    fun getCurrentPointsFlow(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection(REWARD_POINTS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val points = snapshot?.toObject(FirebaseRewardPoints::class.java)?.currentPoints ?: 0
                trySend(points)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Checks and unlocks an achievement for a user.
     */
    suspend fun checkAndUnlockAchievement(userId: String, achievementName: String): Result<Boolean> {
        return try {
            // Check if achievement already exists and is achieved
            val existingAchievement = getAchievementByName(userId, achievementName)
            
            if (existingAchievement != null && existingAchievement.isAchieved()) {
                return Result.success(false) // Already achieved
            }
            
            // Get achievement definition
            val achievementDef = getAchievementDefinition(achievementName)
            if (achievementDef == null) {
                return Result.failure(Exception("Unknown achievement: $achievementName"))
            }
            
            // Mark as achieved
            val achievedAchievement = if (existingAchievement != null) {
                // Update existing
                val updates = mapOf(
                    "achievedDate" to com.google.firebase.Timestamp.now()
                )
                firestore.collection(ACHIEVEMENTS_COLLECTION)
                    .document(existingAchievement.id)
                    .update(updates)
                    .await()
                true
            } else {
                // Create new achieved achievement
                val newAchievement = FirebaseAchievement.createAchieved(
                    userId = userId,
                    achievementName = achievementName,
                    description = achievementDef.description,
                    iconName = achievementDef.iconName,
                    achievedDate = Date()
                )
                
                firestore.collection(ACHIEVEMENTS_COLLECTION)
                    .document()
                    .set(newAchievement)
                    .await()
                true
            }
            
            Result.success(achievedAchievement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets all achievements for a user.
     */
    fun getUserAchievements(userId: String): Flow<List<FirebaseAchievement>> = callbackFlow {
        val listener = firestore.collection(ACHIEVEMENTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val achievements = snapshot?.toObjects(FirebaseAchievement::class.java) ?: emptyList()
                trySend(achievements)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Gets leaderboard data (users with their points).
     */
    suspend fun getLeaderboard(limit: Int = 50): List<FirebaseRewardPoints> {
        return try {
            // Get top users by points
            val pointsSnapshot = firestore.collection(REWARD_POINTS_COLLECTION)
                .orderBy("currentPoints", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            pointsSnapshot.toObjects(FirebaseRewardPoints::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets leaderboard as Flow for real-time updates.
     */
    fun getLeaderboardFlow(limit: Int = 50): Flow<List<FirebaseRewardPoints>> = callbackFlow {
        val listener = firestore.collection(REWARD_POINTS_COLLECTION)
            .orderBy("currentPoints", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val pointsList = snapshot?.toObjects(FirebaseRewardPoints::class.java) ?: emptyList()
                trySend(pointsList)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Gets user's rank in leaderboard.
     */
    suspend fun getUserRank(userId: String): Int {
        return try {
            val userPoints = getCurrentPoints(userId)
            
            val higherRankedCount = firestore.collection(REWARD_POINTS_COLLECTION)
                .whereGreaterThan("currentPoints", userPoints)
                .get()
                .await()
                .size()
            
            higherRankedCount + 1
        } catch (e: Exception) {
            -1 // Error or not found
        }
    }

    /**
     * Gets user points as Flow for real-time updates.
     */
    fun getUserPointsFlow(userId: String): Flow<FirebaseRewardPoints?> = callbackFlow {
        val listener = firestore.collection(REWARD_POINTS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val points = snapshot?.toObject(FirebaseRewardPoints::class.java)
                trySend(points)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Gets user achievements as Flow for real-time updates.
     */
    fun getUserAchievementsFlow(userId: String): Flow<List<FirebaseAchievement>> = callbackFlow {
        val listener = firestore.collection(ACHIEVEMENTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val achievements = snapshot?.toObjects(FirebaseAchievement::class.java) ?: emptyList()
                trySend(achievements)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Claims daily login reward.
     */
    suspend fun claimDailyLoginReward(userId: String): Result<Int> {
        return try {
            val pointsAwarded = 5 // Daily login reward
            addPoints(userId, pointsAwarded)
            Result.success(pointsAwarded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * TEMPORARY: Deletes user points data (for cleanup purposes).
     */
    suspend fun deleteUserPoints(userId: String): Result<Unit> {
        return try {
            firestore.collection(REWARD_POINTS_COLLECTION)
                .document(userId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets achievement by name for a user.
     */
    private suspend fun getAchievementByName(userId: String, achievementName: String): FirebaseAchievement? {
        return try {
            val snapshot = firestore.collection(ACHIEVEMENTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("achievementName", achievementName)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.toObject(FirebaseAchievement::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Initializes predefined achievements for a new user.
     */
    private suspend fun initializePredefinedAchievements(userId: String) {
        val predefinedAchievements = listOf(
            AchievementDefinition(
                name = Constants.Achievements.FIRST_EXPENSE_LOGGED_FIREBASE_ID,
                description = "Logged your first expense",
                iconName = "ic_first_expense"
            ),
            AchievementDefinition(
                name = Constants.Achievements.BUDGET_CREATED_ID,
                description = "Created your first budget",
                iconName = "ic_budget_created"
            ),
            AchievementDefinition(
                name = Constants.Achievements.WEEK_UNDER_BUDGET_ID,
                description = "Stayed under budget for a week",
                iconName = "ic_week_budget"
            ),
            AchievementDefinition(
                name = Constants.Achievements.MONTH_UNDER_BUDGET_ID,
                description = "Stayed under budget for a month",
                iconName = "ic_month_budget"
            ),
            AchievementDefinition(
                name = Constants.Achievements.HUNDRED_POINTS_ID,
                description = "Earned 100 points",
                iconName = "ic_hundred_points"
            )
        )
        
        predefinedAchievements.forEach { achievementDef ->
            val achievement = FirebaseAchievement.create(
                userId = userId,
                achievementName = achievementDef.name,
                description = achievementDef.description,
                iconName = achievementDef.iconName
            )
            
            firestore.collection(ACHIEVEMENTS_COLLECTION)
                .document()
                .set(achievement)
                .await()
        }
    }

    /**
     * Gets achievement definition by name.
     */
    private fun getAchievementDefinition(achievementName: String): AchievementDefinition? {
        return when (achievementName) {
            Constants.Achievements.FIRST_EXPENSE_LOGGED_FIREBASE_ID -> AchievementDefinition(
                name = achievementName,
                description = "Logged your first expense",
                iconName = "ic_first_expense"
            )
            Constants.Achievements.BUDGET_CREATED_ID -> AchievementDefinition(
                name = achievementName,
                description = "Created your first budget",
                iconName = "ic_budget_created"
            )
            Constants.Achievements.WEEK_UNDER_BUDGET_ID -> AchievementDefinition(
                name = achievementName,
                description = "Stayed under budget for a week",
                iconName = "ic_week_budget"
            )
            Constants.Achievements.MONTH_UNDER_BUDGET_ID -> AchievementDefinition(
                name = achievementName,
                description = "Stayed under budget for a month",
                iconName = "ic_month_budget"
            )
            Constants.Achievements.HUNDRED_POINTS_ID -> AchievementDefinition(
                name = achievementName,
                description = "Earned 100 points",
                iconName = "ic_hundred_points"
            )
            else -> null
        }
    }
}

/**
 * Data class for achievement definitions.
 */
data class AchievementDefinition(
    val name: String,
    val description: String,
    val iconName: String
)

// LeaderboardEntry moved to FirebaseRewardsViewModel to avoid conflicts