package com.example.budgetbuddy.data.firebase.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.util.Date

/**
 * Firebase achievement data model for Firestore storage.
 * This replaces the Room AchievementEntity for cloud storage.
 */
data class FirebaseAchievement(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val achievementName: String = "",
    val description: String = "",
    val iconName: String? = null,
    val achievedDate: Timestamp? = null, // Null if not yet achieved
    val createdAt: Timestamp = Timestamp.now()
) {
    // No-argument constructor required for Firestore
    constructor() : this("")
    
    // Helper methods
    @Exclude
    fun isAchieved(): Boolean = achievedDate != null
    @Exclude
    fun getAchievedDateAsDate(): Date? = achievedDate?.toDate()
    
    companion object {
        fun create(
            userId: String,
            achievementName: String,
            description: String,
            iconName: String? = null
        ): FirebaseAchievement {
            return FirebaseAchievement(
                userId = userId,
                achievementName = achievementName,
                description = description,
                iconName = iconName
            )
        }
        
        fun createAchieved(
            userId: String,
            achievementName: String,
            description: String,
            iconName: String? = null,
            achievedDate: Date = Date()
        ): FirebaseAchievement {
            return FirebaseAchievement(
                userId = userId,
                achievementName = achievementName,
                description = description,
                iconName = iconName,
                achievedDate = Timestamp(achievedDate)
            )
        }
    }
} 