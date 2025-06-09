package com.example.budgetbuddy.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyConverter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: UserPreferencesManager
) {
    
    // Currency symbols mapping
    private val currencySymbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "CAD" to "C$",
        "AUD" to "A$",
        "JPY" to "¥",
        "CNY" to "¥",
        "INR" to "₹",
        "ZAR" to "R" // South African Rand (current default)
    )
    
    /**
     * Get the user's selected currency from preferences
     */
    fun getSelectedCurrency(): String {
        return preferencesManager.getSelectedCurrency()
    }
    
    /**
     * Get the currency symbol for the selected currency
     */
    fun getCurrencySymbol(currency: String = getSelectedCurrency()): String {
        return currencySymbols[currency] ?: "$"
    }
    
    /**
     * Format amount with the user's selected currency symbol (no conversion)
     */
    fun formatAmount(amount: BigDecimal, currency: String = getSelectedCurrency()): String {
        val symbol = getCurrencySymbol(currency)
        return "$symbol${String.format("%.2f", amount)}"
    }
    
    /**
     * Format amount with the user's selected currency symbol (no conversion)
     */
    fun formatAmount(amount: Double, currency: String = getSelectedCurrency()): String {
        val symbol = getCurrencySymbol(currency)
        return "$symbol${String.format("%.2f", amount)}"
    }
    
    /**
     * Get all available currencies
     */
    fun getAvailableCurrencies(): List<String> {
        return currencySymbols.keys.toList().sorted()
    }
    
    /**
     * Get currency display name with symbol
     */
    fun getCurrencyDisplayName(currency: String): String {
        val symbol = getCurrencySymbol(currency)
        return "$currency ($symbol)"
    }
} 