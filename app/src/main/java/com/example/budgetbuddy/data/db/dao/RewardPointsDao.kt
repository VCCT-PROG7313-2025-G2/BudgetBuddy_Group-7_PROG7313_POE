package com.example.budgetbuddy.data.db.dao

import androidx.room.*
import com.example.budgetbuddy.data.db.entity.RewardPointsEntity
import com.example.budgetbuddy.model.UserWithPoints
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardPointsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePoints(points: RewardPointsEntity): Long

    @Query("SELECT * FROM reward_points WHERE userId = :userId LIMIT 1")
    fun getPointsForUser(userId: Long): Flow<RewardPointsEntity?>

    @Query("UPDATE reward_points SET currentPoints = currentPoints + :pointsToAdd WHERE userId = :userId")
    suspend fun addPoints(userId: Long, pointsToAdd: Int)

    @Query("UPDATE reward_points SET currentPoints = :newTotal WHERE userId = :userId")
    suspend fun setPoints(userId: Long, newTotal: Int)

    // Query to get leaderboard entries (User joined with their points, ordered)
    @Transaction // Good practice for queries spanning multiple tables implicitly
    @Query("""
        SELECT u.*,
               rp.pointsId AS points_pointsId,
               rp.userId AS points_userId,
               rp.currentPoints AS points_currentPoints 
        FROM users u
        INNER JOIN reward_points rp ON u.userId = rp.userId
        ORDER BY rp.currentPoints DESC
        LIMIT :limit
    """)
    fun getLeaderboard(limit: Int): Flow<List<UserWithPoints>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialPoints(points: RewardPointsEntity): Long

    @Query("SELECT * FROM reward_points WHERE userId = :userId LIMIT 1")
    fun getPointsForUserFlow(userId: Long): Flow<RewardPointsEntity?>

    @Query("SELECT * FROM reward_points WHERE userId = :userId LIMIT 1")
    suspend fun getPointsForUserOnce(userId: Long): RewardPointsEntity?

    @Query("UPDATE reward_points SET currentPoints = currentPoints + :pointsToAdd WHERE userId = :userId")
    suspend fun incrementPoints(userId: Long, pointsToAdd: Int): Int

    @Transaction
    suspend fun addOrUpdatePoints(userId: Long, pointsToAdd: Int) {
        val insertedRowId = insertInitialPoints(RewardPointsEntity(userId = userId, currentPoints = 0))
        incrementPoints(userId, pointsToAdd)
    }
} 