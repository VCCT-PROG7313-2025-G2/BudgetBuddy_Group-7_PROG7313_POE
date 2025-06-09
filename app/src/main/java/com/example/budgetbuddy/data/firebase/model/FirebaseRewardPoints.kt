package com.example.budgetbuddy.data.firebase.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Firebase reward points data model for Firestore storage.
 * This replaces the Room RewardPointsEntity for cloud storage.
 */
data class FirebaseRewardPoints(
    @DocumentId
    val id: String = "", // Same as userId for easy lookup
    val userId: String = "",
    val currentPoints: Int = 0,
    val lastUpdated: Timestamp = Timestamp.now()
) {
    // No-argument constructor required for Firestore
    constructor() : this("")
    
    companion object {
        fun create(userId: String, initialPoints: Int = 0): FirebaseRewardPoints {
            return FirebaseRewardPoints(
                id = userId, // Use userId as document ID
                userId = userId,
                currentPoints = initialPoints
            )
        }
    }
} 