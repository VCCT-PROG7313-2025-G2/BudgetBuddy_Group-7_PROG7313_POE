package com.example.budgetbuddy

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.budgetbuddy.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class BottomNavigationTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
        // Wait for app to fully load
        try {
            Thread.sleep(2000) // Longer wait for app initialization
        } catch (e: InterruptedException) { }
    }

    @Test
    fun testBottomNavigationExists() {
        // First check if bottom navigation is visible
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToReports() {
        // Wait for bottom navigation to be available
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
        
        // Click Reports icon
        onView(withId(R.id.reportsFragment)).perform(click())
        
        // Small delay for navigation
        try { Thread.sleep(1000) } catch (e: InterruptedException) { }
        
        // Verify we're on reports screen by checking for unique element
        // Use a more generic approach since toolbar structure might vary
        onView(withText("Reports & Insights")).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToRewards() {
        // Wait for bottom navigation
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
        
        // Click Rewards icon
        onView(withId(R.id.rewardsFragment)).perform(click())
        
        try { Thread.sleep(1000) } catch (e: InterruptedException) { }
        
        // Check for rewards screen content
        onView(withText("Rewards & Achievements")).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToProfile() {
        // Wait for bottom navigation
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
        
        // Click Profile icon
        onView(withId(R.id.profileFragment)).perform(click())
        
        try { Thread.sleep(1000) } catch (e: InterruptedException) { }
        
        // Check for profile screen content
        onView(withText("Profile")).check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateBackToHome() {
        // Wait for bottom navigation
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
        
        // Go to Profile first
        onView(withId(R.id.profileFragment)).perform(click())
        try { Thread.sleep(500) } catch (e: InterruptedException) { }
        
        // Go back to Home
        onView(withId(R.id.homeFragment)).perform(click())
        try { Thread.sleep(500) } catch (e: InterruptedException) { }
        
        // Verify Home screen is shown (check for greetingTextView)
        onView(withId(R.id.greetingTextView)).check(matches(isDisplayed()))
    }
} 