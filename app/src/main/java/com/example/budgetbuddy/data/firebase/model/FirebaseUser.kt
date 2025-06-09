package com.example.budgetbuddy.data.firebase.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Firebase user data model for Firestore storage.
 * This replaces the Room UserEntity for cloud storage.
 */
data class FirebaseUser(
    @DocumentId
    val id: String = "", // Firebase Auth UID
    val name: String = "",
    val email: String = "",
    val biometricEnabled: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    // No-argument constructor required for Firestore
    constructor() : this("")
} 