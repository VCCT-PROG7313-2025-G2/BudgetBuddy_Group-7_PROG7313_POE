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

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: UserEntity) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(email, password)
            result.onSuccess {
                 // TODO: Store logged-in user ID (e.g., SessionManager/DataStore)
                _uiState.value = AuthUiState.Success(it)
            }.onFailure {
                _uiState.value = AuthUiState.Error(it.message ?: "Login failed")
            }
        }
    }

    fun signup(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            // TODO: Add password validation logic
            val result = authRepository.signup(name, email, password)
            result.onSuccess {
                 // TODO: Store logged-in user ID (e.g., SessionManager/DataStore)
                // TODO: Initialize points/achievements via RewardsRepository
                _uiState.value = AuthUiState.Success(it)
            }.onFailure {
                _uiState.value = AuthUiState.Error(it.message ?: "Signup failed")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
} 