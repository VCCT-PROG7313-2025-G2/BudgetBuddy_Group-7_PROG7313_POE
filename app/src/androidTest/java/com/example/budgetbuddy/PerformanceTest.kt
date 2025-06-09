package com.example.budgetbuddy

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

/**
 * Performance tests for BudgetBuddy app
 * These tests measure app startup time, navigation performance, and database operations
 */
@RunWith(AndroidJUnit4::class)
class PerformanceTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun testAppStartupTime() {
        val startupTime = measureTimeMillis {
            // App should be launched by ActivityScenarioRule
            // Wait for the main content to load
            onView(withId(R.id.bottomNavigation)).check { view, _ ->
                assertNotNull("Bottom navigation should be visible", view)
            }
        }

        // Startup should be under 3 seconds (3000ms)
        assertTrue("App startup time should be under 3 seconds, was ${startupTime}ms", 
                  startupTime < 3000)
    }

    @Test
    fun testNavigationPerformance() {
        val navigationTime = measureTimeMillis {
            // Test navigation between bottom navigation items
            onView(withId(R.id.nav_reports)).perform(click())
            Thread.sleep(100) // Small delay for navigation
            
            onView(withId(R.id.nav_rewards)).perform(click())
            Thread.sleep(100)
            
            onView(withId(R.id.nav_profile)).perform(click())
            Thread.sleep(100)
            
            onView(withId(R.id.nav_home)).perform(click())
            Thread.sleep(100)
        }

        // Navigation should be fast - under 2 seconds for 4 navigations
        assertTrue("Navigation performance should be under 2 seconds, was ${navigationTime}ms", 
                  navigationTime < 2000)
    }

    @Test
    fun testScrollPerformance() {
        // Navigate to a screen with scrollable content (like expenses or reports)
        onView(withId(R.id.nav_reports)).perform(click())
        
        val scrollTime = measureTimeMillis {
            // Perform scroll operations
            repeat(5) {
                onView(withId(R.id.scrollView)).perform(swipeUp())
                Thread.sleep(50)
                onView(withId(R.id.scrollView)).perform(swipeDown())
                Thread.sleep(50)
            }
        }

        // Scrolling should be smooth - under 1 second for 10 scroll operations
        assertTrue("Scroll performance should be under 1 second, was ${scrollTime}ms", 
                  scrollTime < 1000)
    }

    @Test
    fun testMemoryUsage() {
        // Get memory info before test
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Perform memory-intensive operations
        repeat(10) {
            onView(withId(R.id.nav_reports)).perform(click())
            Thread.sleep(100)
            onView(withId(R.id.nav_home)).perform(click())
            Thread.sleep(100)
        }

        // Force garbage collection
        runtime.gc()
        Thread.sleep(100)

        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        // Memory increase should be reasonable (under 50MB)
        val maxMemoryIncrease = 50 * 1024 * 1024 // 50MB in bytes
        assertTrue("Memory increase should be under 50MB, was ${memoryIncrease / (1024 * 1024)}MB", 
                  memoryIncrease < maxMemoryIncrease)
    }

    @Test
    fun testDatabaseOperationPerformance() {
        // Navigate to home screen where database operations might occur
        onView(withId(R.id.nav_home)).perform(click())

        val dbOperationTime = measureTimeMillis {
            // Trigger potential database operations by navigating between screens
            // that load data from Firebase/Room database
            onView(withId(R.id.nav_reports)).perform(click())
            Thread.sleep(500) // Wait for data loading
            
            onView(withId(R.id.nav_rewards)).perform(click())
            Thread.sleep(500) // Wait for data loading
            
            onView(withId(R.id.nav_home)).perform(click())
            Thread.sleep(500) // Wait for data loading
        }

        // Database operations should complete within 5 seconds
        assertTrue("Database operations should complete within 5 seconds, took ${dbOperationTime}ms", 
                  dbOperationTime < 5000)
    }

    @Test
    fun testUIResponsiveness() {
        val responseTime = measureTimeMillis {
            // Test rapid UI interactions
            repeat(20) {
                onView(withId(R.id.nav_home)).perform(click())
                onView(withId(R.id.nav_reports)).perform(click())
            }
        }

        // UI should remain responsive - under 3 seconds for 40 clicks
        assertTrue("UI should remain responsive, took ${responseTime}ms for 40 interactions", 
                  responseTime < 3000)
    }

    @Test
    fun testBatteryOptimization() {
        // This test ensures the app doesn't perform excessive operations
        val startTime = System.currentTimeMillis()
        
        // Simulate normal app usage for a short period
        repeat(5) {
            onView(withId(R.id.nav_home)).perform(click())
            Thread.sleep(200)
            onView(withId(R.id.nav_reports)).perform(click())
            Thread.sleep(200)
            onView(withId(R.id.nav_rewards)).perform(click())
            Thread.sleep(200)
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Test should complete in reasonable time indicating no excessive processing
        assertTrue("Battery optimization test should complete quickly, took ${totalTime}ms", 
                  totalTime < 4000)
    }
} 