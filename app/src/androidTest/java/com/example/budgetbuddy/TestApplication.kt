package com.example.budgetbuddy

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import android.app.Application

/**
 * Custom test application for Hilt testing.
 * This ensures proper dependency injection during instrumented tests.
 */
class TestApplication : HiltTestApplication() {
    
    override fun onCreate() {
        super.onCreate()
        // Any test-specific initialization can go here
    }
} 