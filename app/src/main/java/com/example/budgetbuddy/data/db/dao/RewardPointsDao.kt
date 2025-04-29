package com.example.budgetbuddy.data.db.dao

import androidx.room.*
import com.example.budgetbuddy.data.db.entity.RewardPointsEntity
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
} 