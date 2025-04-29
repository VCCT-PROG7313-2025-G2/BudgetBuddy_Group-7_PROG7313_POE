package com.example.budgetbuddy.data.db.dao

import androidx.room.*
import com.example.budgetbuddy.data.db.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: AchievementEntity)

    @Update
    suspend fun update(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getAllAchievementsForUser(userId: Long): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE achievementId = :id")
    suspend fun getAchievementById(id: Long): AchievementEntity?

    @Query("SELECT * FROM achievements WHERE achievementId = :achievementId AND userId = :userId LIMIT 1")
    suspend fun getAchievementByIdAndUser(achievementId: Long, userId: Long): AchievementEntity?

    @Query("UPDATE achievements SET achievedDate = :achievedTimestamp WHERE achievementId = :achievementId")
    suspend fun markAchievementAchieved(achievementId: Long, achievedTimestamp: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<AchievementEntity>)
} 