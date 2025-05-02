package com.example.budgetbuddy

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.budgetbuddy.R // Import R class if needed for IDs
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class LoginFlowTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    // Launch the MainActivity before each test
    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        // Inject dependencies
        hiltRule.inject()
        // TODO: Add logic here if needed to ensure the login screen is visible.
        // This might involve logging out if a user is already logged in from a previous test run,
        // or navigating to the login screen if the app doesn't start there.
        // For now, assume the app starts at the login screen if no user is logged in.
         try {
             Thread.sleep(1000) // Wait a bit for initial screen to settle
             // Check if login button is displayed as an indicator of being on the right screen
             onView(withId(R.id.loginButton)).check(matches(isDisplayed()))
         } catch (e: Exception) {
             // If login button isn't found, we might already be logged in or on a different screen.
             // Handle this case - e.g., attempt logout via profile/settings if needed.
             // Log.e("TestSetup", "Initial screen might not be Login/Signup", e)
         }
    }

    @Test
    fun testLoginSuccessfully() {
        // --- IMPORTANT ---
        // Replace "test@example.com" and "password123" with ACTUAL VALID credentials
        // that exist in your test database or backend.
        val userEmail = "test@example.com"
        val userPassword = "password123"

        // 1. Verify Login screen elements are present
        onView(withId(R.id.emailEditText)).check(matches(isDisplayed()))
        onView(withId(R.id.passwordEditText)).check(matches(isDisplayed()))
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()))

        // 2. Enter email and password
        onView(withId(R.id.emailEditText)).perform(typeText(userEmail), closeSoftKeyboard())
        onView(withId(R.id.passwordEditText)).perform(typeText(userPassword), closeSoftKeyboard())

        // 3. Click Login Button
        onView(withId(R.id.loginButton)).perform(click())

        // 4. Verify navigation to the Home screen
        // Add a small delay to allow for navigation and potential UI/data updates
        try {
            Thread.sleep(2000) // Login might take slightly longer
        } catch (e: InterruptedException) { }
        // Check if the greetingTextView (unique to Home) is displayed
        onView(withId(R.id.greetingTextView)).check(matches(isDisplayed()))

        // Optional: Add further checks like verifying the greeting text contains the user's name
    }

    // TODO: Add tests for invalid login attempts (wrong password, non-existent email)
    // TODO: Add test for navigating to SignUp screen
} 