package com.example.budgetbuddy.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-based session manager that uses Firebase Auth state
 * instead of SharedPreferences for user session management.
 * This replaces the traditional SessionManager for Firebase integration.
 */
@Singleton
class FirebaseSessionManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    companion object {
        const val NO_USER_LOGGED_IN = ""
    }

    /**
     * Gets the current user ID from Firebase Auth.
     * Returns empty string if no user is logged in.
     */
    fun getUserId(): String {
        return firebaseAuth.currentUser?.uid ?: NO_USER_LOGGED_IN
    }

    /**
     * Gets the current Firebase user.
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Checks if a user is currently logged in.
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Logs out the current user.
     */
    fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Gets authentication state as Flow for reactive updates.
     * Emits the current user ID whenever auth state changes.
     */
    fun getAuthStateFlow(): Flow<String> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val userId = auth.currentUser?.uid ?: NO_USER_LOGGED_IN
            trySend(userId)
        }
        
        firebaseAuth.addAuthStateListener(listener)
        
        // Send current state immediately
        val currentUserId = firebaseAuth.currentUser?.uid ?: NO_USER_LOGGED_IN
        trySend(currentUserId)
        
        awaitClose { 
            firebaseAuth.removeAuthStateListener(listener) 
        }
    }

    /**
     * Gets user email from Firebase Auth.
     */
    fun getUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    /**
     * Gets user display name from Firebase Auth.
     */
    fun getUserDisplayName(): String? {
        return firebaseAuth.currentUser?.displayName
    }

    /**
     * Checks if the current user's email is verified.
     */
    fun isEmailVerified(): Boolean {
        return firebaseAuth.currentUser?.isEmailVerified ?: false
    }

    /**
     * Clears the session (alias for logout for compatibility).
     */
    fun clearSession() {
        logout()
    }
} 