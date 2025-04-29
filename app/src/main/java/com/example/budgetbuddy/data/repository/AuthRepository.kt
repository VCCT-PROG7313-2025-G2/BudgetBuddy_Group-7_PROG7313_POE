package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.db.dao.UserDao
import com.example.budgetbuddy.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.example.budgetbuddy.data.repository.RewardsRepository
import com.example.budgetbuddy.util.SessionManager

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val rewardsRepository: RewardsRepository,
    private val sessionManager: SessionManager
) {

    // TODO: Implement password hashing (e.g., bcrypt)
    private fun hashPassword(password: String): String {
        // Placeholder - **DO NOT USE IN PRODUCTION**
        return "hashed_" + password
    }

    // TODO: Implement password verification against hash
    private fun verifyPassword(password: String, hash: String): Boolean {
        // Placeholder - **DO NOT USE IN PRODUCTION**
        return hash == hashPassword(password)
    }

    suspend fun login(email: String, password: String): Result<UserEntity> {
        val user = userDao.getUserByEmail(email)
        return if (user != null && verifyPassword(password, user.passwordHash)) {
            sessionManager.saveUserId(user.userId)
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid email or password"))
        }
    }

    suspend fun signup(name: String, email: String, password: String): Result<UserEntity> {
        val existingUser = userDao.getUserByEmail(email)
        if (existingUser != null) {
            return Result.failure(Exception("Email already exists"))
        }
        val hashedPassword = hashPassword(password)
        val newUser = UserEntity(name = name, email = email, passwordHash = hashedPassword)
        val userId = userDao.insertUser(newUser)

        if (userId > 0) { // Check if insert was successful
            // Initialize achievements and points for the new user
            rewardsRepository.initializeAchievementsForUser(userId)
            sessionManager.saveUserId(userId)
        }

        // Return the newly created user (or fetch it again if needed)
        return Result.success(newUser.copy(userId = userId)) // Assuming insert returns ID
    }

    suspend fun changePassword(userId: Long, oldPassword: String, newPassword: String): Result<Unit> {
        val user = userDao.getUserByIdNonFlow(userId) // Need a non-flow version in UserDao
        if (user == null) {
            return Result.failure(Exception("User not found"))
        }

        // Verify old password
        if (!verifyPassword(oldPassword, user.passwordHash)) {
            return Result.failure(Exception("Incorrect old password"))
        }

        // Hash new password and update user
        val newHashedPassword = hashPassword(newPassword)
        val updatedUser = user.copy(passwordHash = newHashedPassword)
        userDao.updateUser(updatedUser)

        return Result.success(Unit)
    }

    fun logout() {
        sessionManager.logout()
        // TODO: Add any other logout cleanup (e.g., clear caches)
    }
} 