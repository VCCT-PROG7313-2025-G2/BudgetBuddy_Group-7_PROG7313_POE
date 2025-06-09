package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.firebase.repository.FirebaseAuthRepository
import com.example.budgetbuddy.data.firebase.model.FirebaseUser
import com.example.budgetbuddy.util.FirebaseSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase-based AuthViewModel that replaces the Room-based AuthViewModel.
 * This demonstrates how to migrate ViewModels to use Firebase repositories
 * while maintaining the same UI interface.
 */

// UI States remain the same for compatibility
sealed class FirebaseAuthUiState {
    object Idle : FirebaseAuthUiState()
    object Loading : FirebaseAuthUiState()
    data class Success(val user: FirebaseUser) : FirebaseAuthUiState()
    data class Error(val message: String) : FirebaseAuthUiState()
}

@HiltViewModel
class FirebaseAuthViewModel @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val sessionManager: FirebaseSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<FirebaseAuthUiState>(FirebaseAuthUiState.Idle)
    val uiState: StateFlow<FirebaseAuthUiState> = _uiState.asStateFlow()

    /**
     * Handles user login with Firebase Auth.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = FirebaseAuthUiState.Loading
            
            val result = firebaseAuthRepository.login(email, password)
            result.onSuccess { user ->
                _uiState.value = FirebaseAuthUiState.Success(user)
            }.onFailure { exception ->
                _uiState.value = FirebaseAuthUiState.Error(
                    exception.message ?: "Login failed"
                )
            }
        }
    }

    /**
     * Handles user signup with Firebase Auth.
     */
    fun signup(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = FirebaseAuthUiState.Loading
            
            // Add basic validation
            if (name.isBlank()) {
                _uiState.value = FirebaseAuthUiState.Error("Name cannot be empty")
                return@launch
            }
            if (!isValidEmail(email)) {
                _uiState.value = FirebaseAuthUiState.Error("Invalid email format")
                return@launch
            }
            if (password.length < 6) {
                _uiState.value = FirebaseAuthUiState.Error("Password must be at least 6 characters")
                return@launch
            }
            
            val result = firebaseAuthRepository.signup(name, email, password)
            result.onSuccess { user ->
                _uiState.value = FirebaseAuthUiState.Success(user)
            }.onFailure { exception ->
                _uiState.value = FirebaseAuthUiState.Error(
                    exception.message ?: "Signup failed"
                )
            }
        }
    }

    /**
     * Changes password for the current user.
     */
    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = FirebaseAuthUiState.Loading
            
            if (newPassword.length < 6) {
                _uiState.value = FirebaseAuthUiState.Error("New password must be at least 6 characters")
                return@launch
            }
            
            val result = firebaseAuthRepository.changePassword(oldPassword, newPassword)
            result.onSuccess {
                // Get current user for success state
                val currentUser = firebaseAuthRepository.getCurrentUserProfile()
                if (currentUser != null) {
                    _uiState.value = FirebaseAuthUiState.Success(currentUser)
                } else {
                    _uiState.value = FirebaseAuthUiState.Error("Password changed but failed to load user profile")
                }
            }.onFailure { exception ->
                _uiState.value = FirebaseAuthUiState.Error(
                    exception.message ?: "Failed to change password"
                )
            }
        }
    }

    /**
     * Updates user profile information.
     */
    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            _uiState.value = FirebaseAuthUiState.Loading
            
            if (name.isBlank()) {
                _uiState.value = FirebaseAuthUiState.Error("Name cannot be empty")
                return@launch
            }
            if (!isValidEmail(email)) {
                _uiState.value = FirebaseAuthUiState.Error("Invalid email format")
                return@launch
            }
            
            val result = firebaseAuthRepository.updateUserProfile(name, email)
            result.onSuccess {
                // Get updated user profile
                val updatedUser = firebaseAuthRepository.getCurrentUserProfile()
                if (updatedUser != null) {
                    _uiState.value = FirebaseAuthUiState.Success(updatedUser)
                } else {
                    _uiState.value = FirebaseAuthUiState.Error("Profile updated but failed to load updated data")
                }
            }.onFailure { exception ->
                _uiState.value = FirebaseAuthUiState.Error(
                    exception.message ?: "Failed to update profile"
                )
            }
        }
    }

    /**
     * Logs out the current user.
     */
    fun logout() {
        sessionManager.logout()
        _uiState.value = FirebaseAuthUiState.Idle
    }

    /**
     * Gets the current user if logged in.
     */
    fun getCurrentUser() {
        viewModelScope.launch {
            val user = firebaseAuthRepository.getCurrentUserProfile()
            if (user != null) {
                _uiState.value = FirebaseAuthUiState.Success(user)
            } else {
                _uiState.value = FirebaseAuthUiState.Idle
            }
        }
    }

    /**
     * Resets the UI state to Idle.
     */
    fun resetState() {
        _uiState.value = FirebaseAuthUiState.Idle
    }

    /**
     * Checks if the user is currently logged in.
     */
    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    /**
     * Basic email validation.
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
} 