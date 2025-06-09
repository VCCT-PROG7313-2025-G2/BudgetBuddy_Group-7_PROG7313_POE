package com.example.budgetbuddy.util

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension functions for common operations in BudgetBuddy app.
 */

/**
 * Safely converts BigDecimal to Int, handling potential overflow.
 */
fun BigDecimal.toIntSafe(): Int {
    return try {
        this.toInt()
    } catch (e: ArithmeticException) {
        // Handle overflow by returning max or min value
        when {
            this > BigDecimal(Int.MAX_VALUE) -> Int.MAX_VALUE
            this < BigDecimal(Int.MIN_VALUE) -> Int.MIN_VALUE
            else -> 0
        }
    }
}

/**
 * Safely converts BigDecimal to Double.
 */
fun BigDecimal.toDoubleSafe(): Double {
    return try {
        this.toDouble()
    } catch (e: Exception) {
        0.0
    }
}

/**
 * Formats Date as display string.
 */
fun Date.toDisplayString(): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(this)
}

/**
 * Formats Date as month-year string for budget storage.
 */
fun Date.toMonthYearString(): String {
    val formatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    return formatter.format(this)
}

/**
 * Formats BigDecimal as currency string using the user's selected currency.
 * Note: This requires a CurrencyConverter instance. For contexts where 
 * CurrencyConverter is not available, use the formatAmount method directly.
 */
fun BigDecimal.toCurrencyString(currencyConverter: CurrencyConverter? = null): String {
    return if (currencyConverter != null) {
        currencyConverter.formatAmount(this)
    } else {
        // Fallback to USD formatting if converter is not available
        "$${String.format("%.2f", this)}"
    }
}

/**
 * Legacy method for backward compatibility - formats with hardcoded "R" symbol.
 * This will be deprecated once all usages are migrated to use CurrencyConverter.
 */
@Deprecated("Use CurrencyConverter.formatAmount() instead for proper currency support")
fun BigDecimal.toCurrencyStringLegacy(): String {
    return "R${String.format("%.2f", this)}"
}

/**
 * Checks if BigDecimal is positive.
 */
fun BigDecimal.isPositive(): Boolean {
    return this > BigDecimal.ZERO
}

/**
 * Checks if BigDecimal is negative.
 */
fun BigDecimal.isNegative(): Boolean {
    return this < BigDecimal.ZERO
}

/**
 * Checks if BigDecimal is zero.
 */
fun BigDecimal.isZero(): Boolean {
    return this.compareTo(BigDecimal.ZERO) == 0
}

/**
 * Returns the absolute value of BigDecimal.
 */
fun BigDecimal.absoluteValue(): BigDecimal {
    return this.abs()
} 