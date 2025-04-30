package com.example.budgetbuddy.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val USER_ID_KEY = "user_id"
        const val NO_USER_LOGGED_IN = -1L // Make public for checks
    }

    fun saveUserId(userId: Long) {
        prefs.edit().putLong(USER_ID_KEY, userId).apply()
    }

    fun getUserId(): Long {
        return prefs.getLong(USER_ID_KEY, NO_USER_LOGGED_IN)
    }

    fun isLoggedIn(): Boolean {
        return getUserId() != NO_USER_LOGGED_IN
    }

    fun logout() {
        prefs.edit().remove(USER_ID_KEY).apply()
    }
} 