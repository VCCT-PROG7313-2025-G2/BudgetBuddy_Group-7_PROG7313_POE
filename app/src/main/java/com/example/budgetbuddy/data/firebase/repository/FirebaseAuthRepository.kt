package com.example.budgetbuddy.data.firebase.repository

import com.example.budgetbuddy.data.firebase.model.FirebaseUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser as FirebaseAuthUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase authentication repository that handles user authentication
 * and user profile management using Firebase Auth + Firestore.
 * This replaces the Room-based AuthRepository.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val USERS_COLLECTION = "users"
    }

    /**
     * Attempts to log in a user with email and password using Firebase Auth.
     * Also fetches user profile data from Firestore.
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                // Fetch user profile from Firestore
                val userProfile = getUserProfile(firebaseUser.uid)
                if (userProfile != null) {
                    Result.success(userProfile)
                } else {
                    Result.failure(Exception("User profile not found"))
                }
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs up a new user with Firebase Auth and creates their profile in Firestore.
     */
    suspend fun signup(name: String, email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseAuthUser = authResult.user
            
            if (firebaseAuthUser != null) {
                // Create user profile in Firestore
                val userProfile = FirebaseUser(
                    id = firebaseAuthUser.uid,
                    name = name,
                    email = email
                )
                
                // Save to Firestore
                firestore.collection(USERS_COLLECTION)
                    .document(firebaseAuthUser.uid)
                    .set(userProfile)
                    .await()
                
                // Note: Rewards will be initialized when user first uses the app
                
                Result.success(userProfile)
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Note: changePassword method is defined later in the class

    /**
     * Updates user profile information in Firestore.
     */
    suspend fun updateUserProfile(name: String, email: String): Result<Unit> {
        return try {
            val user = getCurrentUser()
            if (user == null) {
                return Result.failure(Exception("No user logged in"))
            }
            
            val updates = mapOf(
                "name" to name,
                "email" to email,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .update(updates)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the current user profile from Firestore.
     */
    suspend fun getCurrentUserProfile(): FirebaseUser? {
        val user = getCurrentUser() ?: return null
        return getUserProfile(user.uid)
    }

    /**
     * Gets user profile by ID from Firestore.
     */
    suspend fun getUserProfile(userId: String): FirebaseUser? {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            document.toObject(FirebaseUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets user profile as Flow for reactive updates.
     */
    fun getUserProfileFlow(userId: String): Flow<FirebaseUser?> = flow {
        try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && snapshot.exists()) {
                        // This will be handled by the Flow
                    }
                }
            
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            emit(document.toObject(FirebaseUser::class.java))
        } catch (e: Exception) {
            emit(null)
        }
    }

    /**
     * Logs out the current user.
     */
    fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Gets the current Firebase Auth user.
     */
    fun getCurrentUser(): FirebaseAuthUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Gets the current user ID.
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * Checks if a user is currently logged in.
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Updates user profile with userId parameter.
     */
    suspend fun updateUserProfile(userId: String, name: String, email: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "email" to email,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates biometric authentication setting.
     */
    suspend fun updateBiometricSetting(userId: String, enabled: Boolean): Result<Unit> {
        return try {
            val updates = mapOf(
                "biometricLoginEnabled" to enabled,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Changes password with current password verification.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null) {
                return Result.failure(Exception("No user logged in"))
            }
            
            // Re-authenticate with current password first
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential).await()
            
            // Update password
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes user account with password verification.
     */
    suspend fun deleteAccount(password: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user == null) {
                return Result.failure(Exception("No user logged in"))
            }
            
            // Re-authenticate with password first
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(user.email!!, password)
            user.reauthenticate(credential).await()
            
            // Delete user profile from Firestore
            firestore.collection(USERS_COLLECTION)
                .document(user.uid)
                .delete()
                .await()
            
            // Delete Firebase Auth account
            user.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Sends password reset email.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * TEMPORARY: Deletes user profile data (for cleanup purposes).
     */
    suspend fun deleteUserProfile(userId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 