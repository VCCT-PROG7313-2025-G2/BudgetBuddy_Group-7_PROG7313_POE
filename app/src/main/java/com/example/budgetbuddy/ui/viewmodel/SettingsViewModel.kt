package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.repository.AuthRepository // Import AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SettingsEvent {
    object NavigateToLogin : SettingsEvent()
    // Add other events like ShowError, ShowConfirmationDialog etc.
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Use SharedFlow for one-time events like navigation
    private val _eventFlow = MutableSharedFlow<SettingsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

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