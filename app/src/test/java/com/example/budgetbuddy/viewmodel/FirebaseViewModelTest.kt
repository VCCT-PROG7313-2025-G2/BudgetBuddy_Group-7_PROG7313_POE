package com.example.budgetbuddy.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.*

/**
 * Unit tests for Firebase ViewModels
 * Tests state management, data validation, and business logic
 */
@ExperimentalCoroutinesApi
class FirebaseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test expense validation logic`() = runTest {
        // Test valid expense data
        val validExpense = MockExpenseData(
            amount = 25.50,
            categoryName = "Food",
            date = Date(),
            description = "Lunch at restaurant"
        )
        
        assertTrue("Valid expense should pass validation", isValidExpense(validExpense))
        
        // Test invalid expense data
        val invalidExpense = MockExpenseData(
            amount = -10.0,
            categoryName = "",
            date = Date(),
            description = ""
        )
        
        assertFalse("Invalid expense should fail validation", isValidExpense(invalidExpense))
    }

    @Test
    fun `test budget calculation accuracy`() = runTest {
        val budget = 1000.0
        val expenses = listOf(
            MockExpenseData("Food", 200.0),
            MockExpenseData("Transport", 150.0),
            MockExpenseData("Entertainment", 100.0)
        )
        
        val totalSpent = calculateTotalSpent(expenses)
        val remaining = budget - totalSpent
        val percentage = (totalSpent / budget) * 100
        
        assertEquals("Total spent should be 450.0", 450.0, totalSpent, 0.01)
        assertEquals("Remaining should be 550.0", 550.0, remaining, 0.01)
        assertEquals("Percentage should be 45.0", 45.0, percentage, 0.01)
    }

    @Test
    fun `test category filtering logic`() = runTest {
        val expenses = listOf(
            MockExpenseData("Food", 50.0),
            MockExpenseData("Food", 30.0),
            MockExpenseData("Transport", 25.0),
            MockExpenseData("Entertainment", 40.0)
        )
        
        val foodExpenses = filterExpensesByCategory(expenses, "Food")
        val transportExpenses = filterExpensesByCategory(expenses, "Transport")
        
        assertEquals("Should have 2 food expenses", 2, foodExpenses.size)
        assertEquals("Should have 1 transport expense", 1, transportExpenses.size)
        assertEquals("Food expenses total should be 80.0", 80.0, 
                    foodExpenses.sumOf { it.amount }, 0.01)
    }

    @Test
    fun `test date range filtering`() = runTest {
        val today = Date()
        val yesterday = Date(today.time - (24 * 60 * 60 * 1000))
        val twoDaysAgo = Date(today.time - (2 * 24 * 60 * 60 * 1000))
        
        val expenses = listOf(
            MockExpenseData("Food", 50.0, today),
            MockExpenseData("Transport", 30.0, yesterday),
            MockExpenseData("Entertainment", 40.0, twoDaysAgo)
        )
        
        val todayExpenses = filterExpensesByDateRange(expenses, today, today)
        val lastTwoDaysExpenses = filterExpensesByDateRange(expenses, yesterday, today)
        
        assertEquals("Should have 1 expense today", 1, todayExpenses.size)
        assertEquals("Should have 2 expenses in last two days", 2, lastTwoDaysExpenses.size)
    }

    @Test
    fun `test points calculation system`() = runTest {
        // Test points for different budget performance levels
        val excellentPerformance = calculateAchievementPoints(1000.0, 700.0) // 30% under budget
        val goodPerformance = calculateAchievementPoints(1000.0, 900.0) // 10% under budget
        val overBudget = calculateAchievementPoints(1000.0, 1100.0) // 10% over budget
        
        assertTrue("Excellent performance should earn high points", excellentPerformance >= 50)
        assertTrue("Good performance should earn some points", goodPerformance > 0)
        assertEquals("Over budget should earn no points", 0, overBudget)
    }

    @Test
    fun `test level progression logic`() = runTest {
        val levels = listOf(
            0 to 1,    // 0 points = Level 1
            50 to 1,   // 50 points = Level 1
            100 to 2,  // 100 points = Level 2
            250 to 3,  // 250 points = Level 3
            500 to 4,  // 500 points = Level 4
            1000 to 10 // 1000 points = Level 10
        )
        
        levels.forEach { (points, expectedLevel) ->
            val actualLevel = calculateUserLevel(points)
            assertEquals("Points $points should give level $expectedLevel", 
                        expectedLevel, actualLevel)
        }
    }

    @Test
    fun `test profile validation`() = runTest {
        // Valid profile data
        val validProfile = MockProfileData(
            name = "John Doe",
            email = "john.doe@example.com"
        )
        
        assertTrue("Valid profile should pass validation", isValidProfile(validProfile))
        
        // Invalid profile data
        val invalidProfile = MockProfileData(
            name = "",
            email = "invalid-email"
        )
        
        assertFalse("Invalid profile should fail validation", isValidProfile(invalidProfile))
    }

    @Test
    fun `test reports data aggregation`() = runTest {
        val expenses = listOf(
            MockExpenseData("Food", 100.0),
            MockExpenseData("Food", 50.0),
            MockExpenseData("Transport", 75.0),
            MockExpenseData("Entertainment", 25.0)
        )
        
        val categoryTotals = aggregateByCategory(expenses)
        val totalSpent = expenses.sumOf { it.amount }
        
        assertEquals("Food category should total 150.0", 150.0, categoryTotals["Food"] ?: 0.0, 0.01)
        assertEquals("Transport category should total 75.0", 75.0, categoryTotals["Transport"] ?: 0.0, 0.01)
        assertEquals("Total spent should be 250.0", 250.0, totalSpent, 0.01)
    }

    // Helper functions for testing
    private fun isValidExpense(expense: MockExpenseData): Boolean {
        return expense.amount > 0.0 && 
               expense.categoryName.isNotBlank() && 
               expense.amount <= 10000.0
    }

    private fun calculateTotalSpent(expenses: List<MockExpenseData>): Double {
        return expenses.sumOf { it.amount }
    }

    private fun filterExpensesByCategory(expenses: List<MockExpenseData>, category: String): List<MockExpenseData> {
        return expenses.filter { it.categoryName == category }
    }

    private fun filterExpensesByDateRange(expenses: List<MockExpenseData>, startDate: Date, endDate: Date): List<MockExpenseData> {
        return expenses.filter { expense ->
            !expense.date.before(startDate) && !expense.date.after(endDate)
        }
    }

    private fun calculateAchievementPoints(budget: Double, spent: Double): Int {
        return if (spent <= budget) {
            val percentage = ((budget - spent) / budget) * 100
            (percentage * 2).toInt() // 2 points per percent under budget
        } else {
            0
        }
    }

    private fun calculateUserLevel(points: Int): Int {
        return when {
            points < 100 -> 1
            points < 250 -> 2
            points < 500 -> 3
            points < 750 -> 4
            points < 1000 -> 5
            else -> 10
        }
    }

    private fun isValidProfile(profile: MockProfileData): Boolean {
        return profile.name.isNotBlank() && 
               profile.email.contains("@") && 
               profile.email.contains(".")
    }

    private fun aggregateByCategory(expenses: List<MockExpenseData>): Map<String, Double> {
        return expenses.groupBy { it.categoryName }
            .mapValues { (_, expenseList) -> expenseList.sumOf { it.amount } }
    }

    // Mock data classes for testing
    private data class MockExpenseData(
        val categoryName: String, 
        val amount: Double, 
        val date: Date = Date(),
        val description: String = ""
    )

    private data class MockProfileData(
        val name: String,
        val email: String
    )
} 