package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.repository.AuthRepository
import com.example.budgetbuddy.data.db.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Defines the possible states for the Authentication UI (Login/Signup).
sealed class AuthUiState {
    // Initial state, nothing has happened yet.
    object Idle : AuthUiState()
    // An operation (login/signup) is in progress.
    object Loading : AuthUiState()
    // The operation was successful, includes the user data.
    data class Success(val user: UserEntity) : AuthUiState()
    // An error occurred during the operation.
    data class Error(val message: String) : AuthUiState()
}

// Marks this ViewModel for Hilt injection.
@HiltViewModel
class AuthViewModel @Inject constructor(
    // Hilt provides the AuthRepository instance.
    private val authRepository: AuthRepository
) : ViewModel() {

    // MutableStateFlow holds the current UI state; only the ViewModel can change it.
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    // StateFlow exposes the UI state to the Fragment in a read-only way.
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Function called by the Fragment when the user tries to log in.
    fun login(email: String, password: String) {
        // Launch a coroutine for the background work.
        viewModelScope.launch {
            // Set the state to Loading before starting the operation.
            _uiState.value = AuthUiState.Loading
            // Call the repository to perform the login logic.
            val result = authRepository.login(email, password)
            // Handle the result from the repository.
            result.onSuccess {
                 // TODO: The actual user ID should be saved somewhere globally (like SessionManager).
                 // For now, just update the state to Success, passing the user data.
                _uiState.value = AuthUiState.Success(it)
            }.onFailure {
                // If login fails, update the state to Error with the error message.
                _uiState.value = AuthUiState.Error(it.message ?: "Login failed")
            }
        }
    }

    // Function called by the Fragment when the user tries to sign up.
    fun signup(name: String, email: String, password: String) {
        // Launch a coroutine.
        viewModelScope.launch {
            // Set state to Loading.
            _uiState.value = AuthUiState.Loading
            // TODO: Add password validation logic here before calling repository.
            // Call the repository to perform the signup logic.
            val result = authRepository.signup(name, email, password)
            // Handle the result.
            result.onSuccess {
                 // TODO: Save the new user ID globally (SessionManager).
                 // TODO: Initialize points/achievements via RewardsRepository if needed.
                 // Update state to Success.
                _uiState.value = AuthUiState.Success(it)
            }.onFailure {
                // Update state to Error on failure.
                _uiState.value = AuthUiState.Error(it.message ?: "Signup failed")
            }
        }
    }

    // Resets the UI state back to Idle. Called by the Fragment after handling Success/Error.
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
} 