package com.example.budgetbuddy.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "budget_buddy_prefs", 
        Context.MODE_PRIVATE
    )

    companion object {
        private const val SELECTED_CURRENCY_KEY = "selected_currency"
        private const val DEFAULT_CURRENCY = "USD"
    }

    /**
     * Saves the user's custom minimum budget amount.
     */
    fun setUserMinimumBudget(amount: BigDecimal) {
        sharedPreferences.edit()
            .putString(Constants.Budget.USER_MINIMUM_BUDGET_KEY, amount.toString())
            .apply()
    }

    /**
     * Gets the user's custom minimum budget amount.
     * Returns the default if user hasn't set a custom value.
     */
    fun getUserMinimumBudget(): BigDecimal {
        val savedAmount = sharedPreferences.getString(
            Constants.Budget.USER_MINIMUM_BUDGET_KEY, 
            null
        )
        
        return if (savedAmount != null) {
            try {
                BigDecimal(savedAmount)
            } catch (e: NumberFormatException) {
                Constants.Budget.DEFAULT_MINIMUM_BUDGET_AMOUNT
            }
        } else {
            Constants.Budget.DEFAULT_MINIMUM_BUDGET_AMOUNT
        }
    }

    /**
     * Checks if user has set a custom minimum budget.
     */
    fun hasUserSetMinimumBudget(): Boolean {
        return sharedPreferences.contains(Constants.Budget.USER_MINIMUM_BUDGET_KEY)
    }

    /**
     * Clears the user's custom minimum budget setting.
     */
    fun clearUserMinimumBudget() {
        sharedPreferences.edit()
            .remove(Constants.Budget.USER_MINIMUM_BUDGET_KEY)
            .apply()
    }

    // Currency-related methods

    /**
     * Sets the user's selected currency.
     */
    fun setSelectedCurrency(currency: String) {
        sharedPreferences.edit()
            .putString(SELECTED_CURRENCY_KEY, currency)
            .apply()
    }

    /**
     * Gets the user's selected currency.
     */
    fun getSelectedCurrency(): String {
        return sharedPreferences.getString(SELECTED_CURRENCY_KEY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY
    }

    /**
     * Clears currency preferences.
     */
    fun clearCurrencyPreferences() {
        sharedPreferences.edit()
            .remove(SELECTED_CURRENCY_KEY)
            .apply()
    }
} 