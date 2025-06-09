package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.firebase.model.FirebaseUser
import com.example.budgetbuddy.data.firebase.repository.FirebaseAuthRepository
import com.example.budgetbuddy.util.FirebaseSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val profileUpdateComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val sessionManager: FirebaseSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    fun loadUserProfile() {
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
                    error = e.message ?: "Failed to load user profile"
                )
            }
        }
    }

    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                android.util.Log.d("EditProfileViewModel", "Updating profile: name=$name, email=$email")
                
                val result = authRepository.updateUserProfile(name, email)
                
                if (result.isSuccess) {
                    // Reload user profile to get updated data
                    val userId = sessionManager.getUserId()
                    val updatedProfile = authRepository.getUserProfile(userId)
                    
                    _uiState.value = _uiState.value.copy(
                        user = updatedProfile,
                        isLoading = false,
                        profileUpdateComplete = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to update profile"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("EditProfileViewModel", "Error updating profile", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = when {
                        e.message?.contains("email", ignoreCase = true) == true -> "Email update failed: ${e.message}"
                        e.message?.contains("name", ignoreCase = true) == true -> "Name update failed: ${e.message}"
                        else -> e.message ?: "Failed to update profile"
                    }
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetUpdateComplete() {
        _uiState.value = _uiState.value.copy(profileUpdateComplete = false)
    }
} 