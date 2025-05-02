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
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class ReportsFlowTest {

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
    fun testViewReportsScreen() {
        // 1. Navigate to Reports Screen
        onView(withId(R.id.reportsFragment)).perform(click())

        // 2. Verify Reports screen elements are displayed
        // Add a small delay for fragment transition
        try { Thread.sleep(500) } catch (e: InterruptedException) { }

        // Check Toolbar Title
        onView(allOf(withText(R.string.reports_and_insights_title), isDescendantOfA(withId(R.id.toolbarLayout))))
            .check(matches(isDisplayed()))
        // Check Month Selector TextView
        onView(withId(R.id.monthYearTextView)).check(matches(isDisplayed()))
        // Check Total Spending Card Label
        onView(withId(R.id.totalSpendingLabel)).check(matches(isDisplayed()))
        // Check Pie Chart view exists
        onView(withId(R.id.categoryPieChart)).check(matches(isDisplayed()))
        // Check Bar Chart view exists
        onView(withId(R.id.dailySpendingBarChart)).check(matches(isDisplayed()))
        // Check Download Button
        onView(withId(R.id.downloadReportButton)).check(matches(isDisplayed()))
    }
} 