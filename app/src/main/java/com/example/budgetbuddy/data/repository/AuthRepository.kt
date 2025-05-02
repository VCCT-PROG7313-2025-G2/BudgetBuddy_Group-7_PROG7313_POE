package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.db.dao.UserDao
import com.example.budgetbuddy.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.example.budgetbuddy.data.repository.RewardsRepository
import com.example.budgetbuddy.util.SessionManager

/**
 * Repository responsible for handling user authentication (login, signup, password changes)
 * and managing the user session.
 * It interacts with the UserDao for user data storage and RewardsRepository for initialization.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val rewardsRepository: RewardsRepository,
    private val sessionManager: SessionManager
) {

    // IMPORTANT TODO: Replace placeholders with a strong password hashing library (e.g., bcrypt).
    private fun hashPassword(password: String): String {
        // Placeholder - **DO NOT USE IN PRODUCTION**
        return "hashed_" + password
    }

    // IMPORTANT TODO: Implement verification against the real hash.
    private fun verifyPassword(password: String, hash: String): Boolean {
        // Placeholder - **DO NOT USE IN PRODUCTION**
        return hash == hashPassword(password)
    }

    /**
     * Attempts to log in a user with the given email and password.
     * Verifies credentials and saves the user ID to the session on success.
     * Returns a Result containing the UserEntity on success or an Exception on failure.
     */
    suspend fun login(email: String, password: String): Result<UserEntity> {
        val user = userDao.getUserByEmail(email)
        return if (user != null && verifyPassword(password, user.passwordHash)) {
            // Save user ID to session upon successful login.
            sessionManager.saveUserId(user.userId)
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid email or password"))
        }
    }

    /**
     * Attempts to sign up a new user.
     * Checks if the email already exists, hashes the password, creates the user,
     * initializes rewards, and saves the user ID to the session.
     * Returns a Result containing the new UserEntity on success or an Exception on failure.
     */
    suspend fun signup(name: String, email: String, password: String): Result<UserEntity> {
        val existingUser = userDao.getUserByEmail(email)
        if (existingUser != null) {
            return Result.failure(Exception("Email already exists"))
        }
        val hashedPassword = hashPassword(password)
        val newUser = UserEntity(name = name, email = email, passwordHash = hashedPassword)
        val userId = userDao.insertUser(newUser) // Insert the user into the database.

        // If user creation was successful (got a valid ID)...
        if (userId > 0) {
            // Set up initial achievements/points for the new user.
            rewardsRepository.initializeAchievementsForUser(userId)
            // Log the new user in by saving their ID to the session.
            sessionManager.saveUserId(userId)
        }

        // Return the created user entity (with the generated ID).
        return Result.success(newUser.copy(userId = userId))
    }

    /**
     * Changes the password for a given user ID.
     * Verifies the old password before updating to the new hashed password.
     * Requires a non-Flow version of getUserById in UserDao.
     * Returns Result.success(Unit) or Result.failure(Exception).
     */
    suspend fun changePassword(userId: Long, oldPassword: String, newPassword: String): Result<Unit> {
        val user = userDao.getUserByIdNonFlow(userId) // Fetch user data directly.
        if (user == null) {
            return Result.failure(Exception("User not found"))
        }

        // Check if the provided old password is correct.
        if (!verifyPassword(oldPassword, user.passwordHash)) {
            return Result.failure(Exception("Incorrect old password"))
        }

        // Hash the new password and update the user record in the database.
        val newHashedPassword = hashPassword(newPassword)
        val updatedUser = user.copy(passwordHash = newHashedPassword)
        userDao.updateUser(updatedUser)

        return Result.success(Unit)
    }

    /**
     * Logs the current user out by clearing their ID from the session.
     */
    fun logout() {
        sessionManager.logout()
        // TODO: Add any other logout cleanup (e.g., clear specific caches).
    }
} 