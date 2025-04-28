package com.example.budgetbuddy.data.db.dao

import androidx.room.*
import com.example.budgetbuddy.data.db.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity): Long

    @Update
    suspend fun update(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getAllAchievementsForUser(userId: Long): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE achievementId = :id")
    suspend fun getAchievementById(id: Long): AchievementEntity?

    @Query("UPDATE achievements SET achievedDate = :date WHERE achievementId = :id")
    suspend fun markAchievementAchieved(id: Long, date: Long) // Use Long for Date timestamp
} 