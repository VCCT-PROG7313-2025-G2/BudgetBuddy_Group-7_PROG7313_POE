package com.example.budgetbuddy.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    val name: String,
    val email: String, // Should be unique
    val passwordHash: String // Store a secure hash, not the plain password!
) 