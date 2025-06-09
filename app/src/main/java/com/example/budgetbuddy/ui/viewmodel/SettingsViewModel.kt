package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.firebase.repository.FirebaseAuthRepository
import com.example.budgetbuddy.data.firebase.model.FirebaseUser
import com.example.budgetbuddy.util.FirebaseSessionManager
import com.example.budgetbuddy.util.UserPreferencesManager
import com.example.budgetbuddy.util.CurrencyConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: FirebaseUser? = null,
    val budgetAlertsEnabled: Boolean = true,
    val dailyRemindersEnabled: Boolean = true,
    val autoSyncEnabled: Boolean = true,
    val syncFrequency: String = "Every 2 hours",
    val selectedCurrency: String = "USD",
    val signOutComplete: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currencyChangeSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val sessionManager: FirebaseSessionManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val currencyConverter: CurrencyConverter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        loadUserPreferences()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val userId = sessionManager.getUserId()
                if (userId.isNotEmpty()) {
                    val userProfile = authRepository.getUserProfile(userId)
                    _uiState.value = _uiState.value.copy(
                        user = userProfile,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No user session found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load user data"
                )
            }
        }
    }

    private fun loadUserPreferences() {
        // Load preferences from UserPreferencesManager
        _uiState.value = _uiState.value.copy(
            budgetAlertsEnabled = true, // TODO: Load from preferences
            dailyRemindersEnabled = true, // TODO: Load from preferences
            autoSyncEnabled = true, // TODO: Load from preferences
            syncFrequency = "Every 2 hours", // TODO: Load from preferences
            selectedCurrency = userPreferencesManager.getSelectedCurrency()
        )
    }

    fun setBudgetAlertsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(budgetAlertsEnabled = enabled)
        savePreference("budget_alerts", enabled)
    }

    fun setDailyRemindersEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dailyRemindersEnabled = enabled)
        savePreference("daily_reminders", enabled)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled)
        savePreference("auto_sync", enabled)
    }

    fun setSyncFrequency(frequency: String) {
        _uiState.value = _uiState.value.copy(syncFrequency = frequency)
        savePreference("sync_frequency", frequency)
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            try {
                // Update preferences
                userPreferencesManager.setSelectedCurrency(currency)
                
                // Update UI state
                _uiState.value = _uiState.value.copy(
                    selectedCurrency = currency,
                    currencyChangeSuccess = true
                )
                
                android.util.Log.d("SettingsViewModel", "Currency changed to: $currency")
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to change currency: ${e.message}"
                )
                android.util.Log.e("SettingsViewModel", "Error changing currency", e)
            }
        }
    }

    private fun savePreference(key: String, value: Any) {
        // In a real app, save to SharedPreferences or sync to Firestore
        android.util.Log.d("SettingsViewModel", "Saving preference: $key = $value")
        // TODO: Implement actual preference saving
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Perform any necessary cleanup or data sync before signing out
                android.util.Log.d("SettingsViewModel", "Performing sign out...")
                
                // Sign out from Firebase
                authRepository.signOut()
                
                // Clear session
                sessionManager.clearSession()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    signOutComplete = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to sign out"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearCurrencyChangeSuccess() {
        _uiState.value = _uiState.value.copy(currencyChangeSuccess = false)
    }

    fun refreshUserData() {
        loadUserData()
    }

    /**
     * Get available currencies from the converter
     */
    fun getAvailableCurrencies(): List<String> {
        return currencyConverter.getAvailableCurrencies()
    }

    /**
     * Get display name for a currency (includes symbol)
     */
    fun getCurrencyDisplayName(currency: String): String {
        return currencyConverter.getCurrencyDisplayName(currency)
    }
} 