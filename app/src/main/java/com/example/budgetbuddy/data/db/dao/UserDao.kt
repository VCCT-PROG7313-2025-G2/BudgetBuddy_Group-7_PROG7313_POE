package com.example.budgetbuddy.data.db.dao

import androidx.room.*
import com.example.budgetbuddy.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if email exists (handle conflict elsewhere)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUserById(userId: Long): Flow<UserEntity?> // Flow for observing changes

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserByIdNonFlow(userId: Long): UserEntity? // Add non-flow version
} 