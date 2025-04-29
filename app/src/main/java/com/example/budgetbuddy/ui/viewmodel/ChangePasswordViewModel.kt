package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.repository.AuthRepository // Use AuthRepository for password logic
import com.example.budgetbuddy.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        val userId = sessionManager.getUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            _uiState.update { it.copy(error = "User not found") }
            return
        }

        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(error = "New passwords do not match") }
            return
        }

        if (newPassword.length < 6) { // Example: Basic length validation
            _uiState.update { it.copy(error = "New password must be at least 6 characters") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // IMPORTANT: This requires adding password change logic to AuthRepository
                // The current AuthRepository only has login/signup placeholders.
                // We need a method like `authRepository.changePassword(userId, oldPassword, newPassword)`

                // *** Placeholder Logic - Requires AuthRepository Implementation ***
                val changeResult = authRepository.changePassword(userId, oldPassword, newPassword)
                if (changeResult.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = changeResult.exceptionOrNull()?.message ?: "Incorrect old password or failed to update") }
                }
                // ******************************************************************

            } catch (e: Exception) {
                Log.e("ChangePasswordViewModel", "Error changing password", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to change password") }
            }
        }
    }
} 