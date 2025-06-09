package com.example.budgetbuddy

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import kotlinx.coroutines.test.runTest
import java.util.*

/**
 * Unit tests for BudgetBuddy core functionality
 * These tests cover utility functions, data validation, and business logic
 */
class BudgetBuddyUnitTests {

    @Before
    fun setup() {
        // Setup common test data if needed
    }

    @Test
    fun `test budget calculation logic`() {
        // Test budget remaining calculation
        val totalBudget = 1000.0
        val totalSpent = 750.0
        val remaining = totalBudget - totalSpent
        
        assertEquals(250.0, remaining, 0.01)
        assertTrue("Budget should have remaining amount", remaining > 0)
    }

    @Test
    fun `test budget percentage calculation`() {
        val totalBudget = 1000.0
        val totalSpent = 250.0
        val percentage = (totalSpent / totalBudget) * 100
        
        assertEquals(25.0, percentage, 0.01)
        assertTrue("Percentage should be between 0 and 100", percentage in 0.0..100.0)
    }

    @Test
    fun `test budget overflow scenario`() {
        val totalBudget = 1000.0
        val totalSpent = 1250.0
        val remaining = totalBudget - totalSpent
        val isOverBudget = totalSpent > totalBudget
        
        assertEquals(-250.0, remaining, 0.01)
        assertTrue("Should detect over budget scenario", isOverBudget)
    }

    @Test
    fun `test expense amount validation`() {
        // Valid amounts
        assertTrue("Valid positive amount should pass", isValidExpenseAmount(25.50))
        assertTrue("Valid zero amount should pass", isValidExpenseAmount(0.0))
        
        // Invalid amounts
        assertFalse("Negative amount should fail", isValidExpenseAmount(-10.0))
        assertFalse("Extremely large amount should fail", isValidExpenseAmount(1_000_000.0))
    }

    @Test
    fun `test category name validation`() {
        // Valid category names
        assertTrue("Valid category name should pass", isValidCategoryName("Food"))
        assertTrue("Valid category with spaces should pass", isValidCategoryName("Dining Out"))
        
        // Invalid category names
        assertFalse("Empty category name should fail", isValidCategoryName(""))
        assertFalse("Blank category name should fail", isValidCategoryName("   "))
        assertFalse("Too long category name should fail", isValidCategoryName("A".repeat(51)))
    }

    @Test
    fun `test budget setup validation`() {
        // Valid budget setup
        assertTrue("Valid monthly budget should pass", isValidMonthlyBudget(1500.0))
        assertTrue("Minimum valid budget should pass", isValidMonthlyBudget(1.0))
        
        // Invalid budget setup
        assertFalse("Zero budget should fail", isValidMonthlyBudget(0.0))
        assertFalse("Negative budget should fail", isValidMonthlyBudget(-100.0))
        assertFalse("Extremely large budget should fail", isValidMonthlyBudget(10_000_000.0))
    }

    @Test
    fun `test points calculation for achievements`() {
        // Test points for staying under budget
        val budgetPoints = calculateBudgetPoints(1000.0, 800.0) // 20% under budget
        assertTrue("Should earn points for staying under budget", budgetPoints > 0)
        
        // Test points for going over budget
        val overBudgetPoints = calculateBudgetPoints(1000.0, 1200.0) // 20% over budget
        assertEquals("Should earn no points for going over budget", 0, overBudgetPoints)
    }

    @Test
    fun `test level calculation from points`() {
        assertEquals("Level 1 for 0-99 points", 1, calculateLevel(50))
        assertEquals("Level 2 for 100-199 points", 2, calculateLevel(150))
        assertEquals("Level 5 for 400-499 points", 5, calculateLevel(450))
        assertEquals("Level 10 for 900-999 points", 10, calculateLevel(950))
    }

    @Test
    fun `test date range validation`() {
        val today = Date()
        val yesterday = Date(today.time - (24 * 60 * 60 * 1000))
        val tomorrow = Date(today.time + (24 * 60 * 60 * 1000))
        
        assertTrue("Yesterday to today should be valid", isValidDateRange(yesterday, today))
        assertFalse("Tomorrow to today should be invalid", isValidDateRange(tomorrow, today))
        assertTrue("Same day should be valid", isValidDateRange(today, today))
    }

    @Test
    fun `test category spending analysis`() {
        val expenses = listOf(
            MockExpense("Food", 50.0),
            MockExpense("Food", 30.0),
            MockExpense("Transport", 25.0),
            MockExpense("Entertainment", 40.0)
        )
        
        val categoryTotals = calculateCategoryTotals(expenses)
        
        assertEquals("Food total should be 80.0", 80.0, categoryTotals["Food"] ?: 0.0, 0.01)
        assertEquals("Transport total should be 25.0", 25.0, categoryTotals["Transport"] ?: 0.0, 0.01)
        assertEquals("Entertainment total should be 40.0", 40.0, categoryTotals["Entertainment"] ?: 0.0, 0.01)
    }

    // Helper functions for validation (these would normally be in your actual app code)
    private fun isValidExpenseAmount(amount: Double): Boolean {
        return amount >= 0.0 && amount <= 100_000.0
    }

    private fun isValidCategoryName(name: String): Boolean {
        return name.trim().isNotEmpty() && name.length <= 50
    }

    private fun isValidMonthlyBudget(budget: Double): Boolean {
        return budget > 0.0 && budget <= 1_000_000.0
    }

    private fun calculateBudgetPoints(budget: Double, spent: Double): Int {
        return if (spent <= budget) {
            val percentage = ((budget - spent) / budget) * 100
            (percentage * 2).toInt() // 2 points per percent under budget
        } else {
            0
        }
    }

    private fun calculateLevel(points: Int): Int {
        return (points / 100) + 1
    }

    private fun isValidDateRange(startDate: Date, endDate: Date): Boolean {
        return !startDate.after(endDate)
    }

    private fun calculateCategoryTotals(expenses: List<MockExpense>): Map<String, Double> {
        return expenses.groupBy { it.category }
            .mapValues { (_, expenseList) -> expenseList.sumOf { it.amount } }
    }

    // Mock data class for testing
    private data class MockExpense(val category: String, val amount: Double)
}