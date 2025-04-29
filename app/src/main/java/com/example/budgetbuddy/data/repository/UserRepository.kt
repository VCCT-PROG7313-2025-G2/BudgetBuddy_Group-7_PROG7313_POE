package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.db.dao.UserDao
import com.example.budgetbuddy.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    // Assuming user ID is stored somewhere (e.g., SessionManager, DataStore)
    // For now, we'll hardcode or pass it around.
    // A better approach is needed for multi-user scenarios.
    fun getUser(userId: Long): Flow<UserEntity?> = userDao.getUserById(userId)

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    // Add other user-related methods if needed (e.g., change password)
} 