package com.example.budgetbuddy.data.repository

import com.example.budgetbuddy.data.db.dao.UserDao
import com.example.budgetbuddy.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao
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
        // Return the newly created user (or fetch it again if needed)
        return Result.success(newUser.copy(userId = userId)) // Assuming insert returns ID
    }
} 