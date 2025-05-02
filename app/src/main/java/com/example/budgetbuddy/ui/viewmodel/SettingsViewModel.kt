package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.repository.AuthRepository // Import AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.budgetbuddy.data.repository.UserRepository
import com.example.budgetbuddy.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.util.Log

sealed class SettingsEvent {
    object NavigateToLogin : SettingsEvent()
    // Add other events like ShowError, ShowConfirmationDialog etc.
}

// Add data class for UI State
data class SettingsUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val userEmail: String = "",
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository, // Inject UserRepository
    private val sessionManager: SessionManager  // Inject SessionManager
) : ViewModel() {

    // Use SharedFlow for one-time events like navigation
    private val _eventFlow = MutableSharedFlow<SettingsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Add StateFlow for UI data
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUserData() // Load user data when ViewModel is created
    }

    private fun loadUserData() {
        val userId = sessionManager.getUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            Log.w("SettingsViewModel", "No user logged in, cannot load user data.")
            _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                userRepository.getUser(userId).collect { user ->
                    if (user != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                userName = user.name ?: "N/A", // Use "N/A" if name is null
                                userEmail = user.email,
                                error = null
                            )
                        }
                    } else {
                        Log.w("SettingsViewModel", "User data not found for ID: $userId")
                        _uiState.update { it.copy(isLoading = false, error = "Failed to load user data") }
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading user data", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "An unknown error occurred") }
            }
        }
    }

    fun onSignOutClicked() {
        viewModelScope.launch {
            // Perform logout operations (clear session)
            authRepository.logout()
            // Emit navigation event
            _eventFlow.emit(SettingsEvent.NavigateToLogin)
        }
    }

    // TODO: Add functions for other settings (notifications, sync, theme, delete account)
} 