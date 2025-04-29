package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.repository.UserRepository
import com.example.budgetbuddy.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val currentName: String = "",
    val currentEmail: String = "",
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val userId = sessionManager.getUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            _uiState.update { it.copy(error = "User not found") }
            return
        }
        viewModelScope.launch {
            userRepository.getUser(userId).collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(currentName = user.name ?: "", currentEmail = user.email)
                    }
                }
            }
        }
    }

    fun saveProfile(newName: String, newEmail: String) {
        val userId = sessionManager.getUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            _uiState.update { it.copy(error = "Cannot save, user not found") }
            return
        }
        // TODO: Add validation (e.g., email format)
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // Fetch current user first to update
                val currentUser = userRepository.getUser(userId).first()
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(name = newName, email = newEmail)
                    userRepository.updateUser(updatedUser)
                    _uiState.update { it.copy(isLoading = false, isSuccess = true, error = null) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load user to update") }
                }
            } catch (e: Exception) {
                Log.e("EditProfileViewModel", "Error saving profile", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save profile") }
            }
        }
    }
} 