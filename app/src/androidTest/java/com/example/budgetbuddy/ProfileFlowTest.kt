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
class ProfileFlowTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
        // Ensure user is logged in and on a screen with bottom nav visible (e.g., Home)
        // TODO: Add login logic if needed, or assume already logged in.
        try {
            Thread.sleep(1500) // Wait for potential initial navigation/login check
            onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
        } catch (e: Exception) {
            // Handle if bottom nav is not visible (might need login first)
        }
    }

    @Test
    fun testViewProfileScreen() {
        // 1. Navigate to Profile Screen
        onView(withId(R.id.profileFragment)).perform(click())

        // 2. Verify Profile screen elements are displayed
        // Add a small delay for fragment transition
        try { Thread.sleep(500) } catch (e: InterruptedException) { }

        // Check for profile image
        onView(withId(R.id.profileImageView)).check(matches(isDisplayed()))
        // Check for name text view
        onView(withId(R.id.profileNameTextView)).check(matches(isDisplayed()))
        // Check for email text view
        onView(withId(R.id.profileEmailTextView)).check(matches(isDisplayed()))
        // Check for budget overview card
        onView(withId(R.id.budgetOverviewCard)).check(matches(isDisplayed()))
        // Check for settings row link
        onView(withId(R.id.settingsRow)).check(matches(isDisplayed()))
         // Check for edit profile button
        onView(withId(R.id.editProfileButton)).check(matches(isDisplayed()))
    }

    // TODO: Add test for navigating to Edit Profile screen
    // TODO: Add test for navigating to Settings screen from Profile
} 