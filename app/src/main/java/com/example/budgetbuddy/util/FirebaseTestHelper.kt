package com.example.budgetbuddy.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Helper class to test Firebase connectivity and initialization.
 * This can be used during development to verify Firebase setup.
 */
object FirebaseTestHelper {
    private const val TAG = "FirebaseTestHelper"

    /**
     * Tests Firebase Auth connectivity.
     */
    suspend fun testFirebaseAuth(): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance()
            Log.d(TAG, "Firebase Auth initialized: ${auth != null}")
            Log.d(TAG, "Current user: ${auth.currentUser?.uid ?: "None"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth test failed", e)
            false
        }
    }

    /**
     * Tests Firestore connectivity.
     */
    suspend fun testFirestore(): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            
            // Try to write a test document
            val testData = mapOf(
                "test" to "connectivity",
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("test")
                .document("connectivity")
                .set(testData)
                .await()
            
            Log.d(TAG, "Firestore connectivity test: SUCCESS")
            
            // Clean up test document
            firestore.collection("test")
                .document("connectivity")
                .delete()
                .await()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firestore test failed", e)
            false
        }
    }

    /**
     * Tests Firebase Storage connectivity.
     */
    suspend fun testFirebaseStorage(): Boolean {
        return try {
            val storage = FirebaseStorage.getInstance()
            val reference = storage.reference.child("test/connectivity.txt")
            
            // Try to upload a small test file
            val testData = "Firebase Storage connectivity test".toByteArray()
            reference.putBytes(testData).await()
            
            Log.d(TAG, "Firebase Storage connectivity test: SUCCESS")
            
            // Clean up test file
            reference.delete().await()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Storage test failed", e)
            false
        }
    }

    /**
     * Runs all Firebase connectivity tests.
     */
    suspend fun testAllFirebaseServices(): Map<String, Boolean> {
        Log.d(TAG, "Starting Firebase connectivity tests...")
        
        val results = mapOf(
            "auth" to testFirebaseAuth(),
            "firestore" to testFirestore(),
            "storage" to testFirebaseStorage()
        )
        
        Log.d(TAG, "Firebase connectivity test results: $results")
        return results
    }

    /**
     * Logs current Firebase configuration.
     */
    fun logFirebaseConfig() {
        try {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            val storage = FirebaseStorage.getInstance()
            
            Log.d(TAG, "=== Firebase Configuration ===")
            Log.d(TAG, "Auth App: ${auth.app.name}")
            Log.d(TAG, "Firestore App: ${firestore.app.name}")
            Log.d(TAG, "Storage App: ${storage.app.name}")
            Log.d(TAG, "Storage Bucket: ${storage.reference.bucket}")
            Log.d(TAG, "==============================")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log Firebase config", e)
        }
    }
} 