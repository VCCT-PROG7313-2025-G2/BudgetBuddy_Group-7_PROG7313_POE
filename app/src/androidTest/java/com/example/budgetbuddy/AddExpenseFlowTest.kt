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
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import android.widget.DatePicker
import androidx.test.espresso.contrib.PickerActions // For DatePicker
import org.hamcrest.Matchers

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class AddExpenseFlowTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    // Launch the MainActivity before each test
    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        // Inject dependencies
        hiltRule.inject()
        // Ensure we are on the home screen initially (optional but good practice)
        // May need a short delay if navigating immediately after launch is flaky
         try {
             Thread.sleep(1000) // Add a small delay if needed after launch
             onView(withId(R.id.addExpenseButton)).check(matches(isDisplayed()))
         } catch (e: Exception) {
             // Handle potential initial screen state difference if not home
             // Log.e("TestSetup", "Initial screen might not be Home", e)
         }
    }

    @Test
    fun testAddExpenseSuccessfully() {
        // 1. Navigate from Home Screen to Add Expense Screen
        onView(withId(R.id.addExpenseButton)).perform(click())

        // 2. Verify Add Expense screen is displayed by checking the save button
        onView(withId(R.id.saveExpenseButton)).check(matches(isDisplayed()))

        // 3. Enter expense details
        // Amount
        onView(withId(R.id.amountEditText)).perform(typeText("75.50"), closeSoftKeyboard())

        // Date (Click icon, then OK on dialog with default date)
        // Click the end icon within the dateInputLayout
        onView(allOf(isDescendantOfA(withId(R.id.dateInputLayout)), withClassName(Matchers.endsWith("CheckableImageButton"))))
            .perform(click())
        // Click OK on the DatePickerDialog
        onView(withText(android.R.string.ok))
            .inRoot(isDialog())
            .perform(click())

        // Category (Click dropdown, then select item)
        onView(withId(R.id.categoryAutoCompleteTextView)).perform(click())
        // Assuming "Groceries" is a valid category in the dropdown list
        // Note: This might be flaky if the list loads slowly.
         try {
             Thread.sleep(500) // Small delay for dropdown items
         } catch (e: InterruptedException) { }
        onView(withText("Groceries"))
           .inRoot(isPlatformPopup()) // Specify that the view is in a popup
           .perform(click())

        // Description/Notes
        onView(withId(R.id.descriptionEditText)).perform(typeText("Test expense groceries"), closeSoftKeyboard())

        // 4. Click Save Button
        onView(withId(R.id.saveExpenseButton)).perform(click())

        // 5. Verify navigation back to the Home screen
        // Add a small delay to allow for navigation and potential UI updates
         try {
             Thread.sleep(1000)
         } catch (e: InterruptedException) { }
        // Check if the addExpenseButton (unique to Home) is displayed again
        onView(withId(R.id.addExpenseButton)).check(matches(isDisplayed()))
    }

    // TODO: Add more tests for edge cases (e.g., invalid input, saving failure)
} 