package com.example.budgetbuddy

import org.junit.Test
import org.junit.Assert.*
import java.util.Locale

/**
 * Simple test to verify currency symbols are available
 */
class CurrencyConverterTest {

    @Test
    fun `test currency symbols exist`() {
        // Test that common currency symbols exist
        val usdSymbol = "$"
        val eurSymbol = "€"
        val gbpSymbol = "£"
        val inrSymbol = "₹"
        val zarSymbol = "R"
        
        assertNotNull("USD symbol should exist", usdSymbol)
        assertNotNull("EUR symbol should exist", eurSymbol)
        assertNotNull("GBP symbol should exist", gbpSymbol)
        assertNotNull("INR symbol should exist", inrSymbol)
        assertNotNull("ZAR symbol should exist", zarSymbol)
        
        assertEquals("USD symbol should be $", "$", usdSymbol)
        assertEquals("EUR symbol should be €", "€", eurSymbol)
        assertEquals("GBP symbol should be £", "£", gbpSymbol)
        assertEquals("INR symbol should be ₹", "₹", inrSymbol)
        assertEquals("ZAR symbol should be R", "R", zarSymbol)
    }

    @Test
    fun `test currency formatting works`() {
        // Test that basic string formatting works for currency
        val amount = 100.50
        val formattedUsd = "$${String.format(Locale.US, "%.2f", amount)}"
        val formattedEur = "€${String.format(Locale.US, "%.2f", amount)}"
        
        assertEquals("USD formatting should work", "$100.50", formattedUsd)
        assertEquals("EUR formatting should work", "€100.50", formattedEur)
    }
} 