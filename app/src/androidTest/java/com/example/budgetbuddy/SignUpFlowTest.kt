package com.example.budgetbuddy

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
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
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class SignUpFlowTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        hiltRule.inject()
        // TODO: Ensure login/signup screen is visible (similar logic as LoginFlowTest)
         try {
             Thread.sleep(1000)
             onView(withId(R.id.loginButton)).check(matches(isDisplayed())) // Check for login button
         } catch (e: Exception) {
             // Handle if not on login screen
         }
    }

    @Test
    fun testSignUpSuccessfully() {
        // Generate a unique email for each test run to avoid conflicts
        val uniqueEmail = "testuser_${UUID.randomUUID()}@example.com"
        val userName = "Test User"
        val password = "Password123"

        // 1. Navigate from Login/Sign Up screen to Account Creation screen
        // Use the signUpButton ID from fragment_login_signup.xml
        onView(withId(R.id.signUpButton)).perform(click())

        // 2. Verify Account Creation screen is displayed
        onView(withId(R.id.fullNameEditText)).check(matches(isDisplayed()))

        // 3. Enter user details
        onView(withId(R.id.fullNameEditText)).perform(typeText(userName), closeSoftKeyboard())
        // Use the emailEditText ID from fragment_account_creation.xml
        onView(withId(R.id.emailEditText)).perform(typeText(uniqueEmail), closeSoftKeyboard())
        // Use the passwordEditText ID from fragment_account_creation.xml
        onView(withId(R.id.passwordEditText)).perform(typeText(password), closeSoftKeyboard())
        onView(withId(R.id.confirmPasswordEditText)).perform(typeText(password), closeSoftKeyboard())

        // 4. Accept terms
        onView(withId(R.id.termsCheckBox)).perform(click())

        // 5. Click Sign Up Button (use ID from fragment_account_creation.xml)
        onView(withId(R.id.signUpButton)).perform(click())

        // 6. Verify navigation to the Home screen
        try {
            Thread.sleep(2500) // Sign up might involve database writes, allow more time
        } catch (e: InterruptedException) { }
        onView(withId(R.id.greetingTextView)).check(matches(isDisplayed()))

        // Optional: Check if greeting text contains the new user's name
        // onView(withId(R.id.greetingTextView)).check(matches(withText(containsString(userName))))
    }

     // TODO: Add tests for sign up failures (email exists, password mismatch, terms not checked)
} 