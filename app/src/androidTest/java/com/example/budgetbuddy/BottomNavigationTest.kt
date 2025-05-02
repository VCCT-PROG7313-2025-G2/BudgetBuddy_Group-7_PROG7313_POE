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
import org.hamcrest.Matchers.allOf // Import allOf
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
        // Ensure user is logged in and on a screen with bottom nav visible (e.g., Home)
        // TODO: Add login logic if needed, or assume already logged in.
        try {
            Thread.sleep(1500) // Wait for potential initial navigation/login check
            // Check if bottom nav is displayed
            onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
        } catch (e: Exception) {
            // Handle if bottom nav is not visible (might need login first)
            // Log.e("TestSetup", "Bottom navigation not initially visible", e)
        }
    }

    @Test
    fun testNavigateToReports() {
        // Click Reports icon
        onView(withId(R.id.reportsFragment)).perform(click())
        // Verify Reports screen is shown (e.g., check for toolbar title or a unique element)
        // Using the toolbar title which is part of the standard setup
        onView(allOf(withText(R.string.reports_and_insights_title), isDescendantOfA(withId(R.id.toolbarLayout))))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToRewards() {
        // Click Rewards icon
        onView(withId(R.id.rewardsFragment)).perform(click())
        // Verify Rewards screen is shown
        onView(allOf(withText(R.string.rewards_and_achievements_title), isDescendantOfA(withId(R.id.toolbarLayout))))
             .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToProfile() {
        // Click Profile icon
        onView(withId(R.id.profileFragment)).perform(click())
         // Verify Profile screen is shown
        onView(allOf(withText(R.string.profile_title), isDescendantOfA(withId(R.id.toolbarLayout))))
             .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateBackToHome() {
        // Go to Profile first
        onView(withId(R.id.profileFragment)).perform(click())
        try { Thread.sleep(500) } catch (e: InterruptedException) { }
        // Go back to Home
        onView(withId(R.id.homeFragment)).perform(click())
        // Verify Home screen is shown (check for greetingTextView)
        try { Thread.sleep(500) } catch (e: InterruptedException) { }
        onView(withId(R.id.greetingTextView)).check(matches(isDisplayed()))
    }
} 