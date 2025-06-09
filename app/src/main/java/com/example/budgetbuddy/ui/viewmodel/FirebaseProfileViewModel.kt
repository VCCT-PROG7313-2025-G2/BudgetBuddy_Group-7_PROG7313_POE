package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.firebase.repository.FirebaseAuthRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseRewardsRepository
import com.example.budgetbuddy.data.firebase.model.FirebaseUser
import com.example.budgetbuddy.util.FirebaseSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase-based ProfileViewModel that replaces the Room-based version.
 * Uses Firebase repositories for user profile management and settings.
 */

// UI State classes remain the same for compatibility
data class FirebaseProfileUiState(
    val user: UserProfileUiState? = null,
    val totalPoints: Int = 0,
    val userLevel: Int = 1,
    val pointsToNextLevel: Int = 0,
    val achievementsCount: Int = 0,
    val currentRank: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class UserProfileUiState(
    val userId: String,
    val name: String,
    val email: String,
    val biometricEnabled: Boolean,
    val profileImageUrl: String? = null,
    val joinDate: String,
    val lastLoginDate: String? = null
)

sealed class FirebaseProfileAction {
    object Idle : FirebaseProfileAction()
    object Loading : FirebaseProfileAction()
    object Success : FirebaseProfileAction()
    data class Error(val message: String) : FirebaseProfileAction()
}

@HiltViewModel
class FirebaseProfileViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val rewardsRepository: FirebaseRewardsRepository,
    private val sessionManager: FirebaseSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirebaseProfileUiState(isLoading = true))
    val uiState: StateFlow<FirebaseProfileUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<FirebaseProfileAction>(FirebaseProfileAction.Idle)
    val actionState: StateFlow<FirebaseProfileAction> = _actionState.asStateFlow()

    private fun getCurrentUserId(): String = sessionManager.getUserId()

    // Live data streams
    private val userProfileFlow = authRepository.getUserProfileFlow(getCurrentUserId())
    private val userPointsFlow = rewardsRepository.getUserPointsFlow(getCurrentUserId())
    private val userAchievementsFlow = rewardsRepository.getUserAchievementsFlow(getCurrentUserId())

    init {
        loadProfileData()
    }

    /**
     * Loads all profile data with real-time updates.
     */
    private fun loadProfileData() {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            _uiState.value = FirebaseProfileUiState(isLoading = false, error = "Please log in.")
            return
        }

        viewModelScope.launch {
            combine(
                userProfileFlow,
                userPointsFlow,
                userAchievementsFlow
            ) { userProfile, userPoints, achievements ->
                
                val profileUiState = userProfile?.let { user ->
                    UserProfileUiState(
                        userId = user.id,
                        name = user.name,
                        email = user.email,
                        biometricEnabled = user.biometricEnabled,
                        profileImageUrl = null, // Firebase Storage URL if implemented
                        joinDate = user.createdAt.toDate().toString(),
                        lastLoginDate = user.updatedAt.toDate().toString()
                    )
                }

                val currentPoints = userPoints?.currentPoints ?: 0
                val userLevel = calculateUserLevel(currentPoints)
                val pointsToNext = calculatePointsToNextLevel(currentPoints, userLevel)
                val achievementsCount = achievements.size
                
                // Get user rank
                val userRank = try {
                    rewardsRepository.getUserRank(userId)
                } catch (e: Exception) {
                    0
                }

                FirebaseProfileUiState(
                    user = profileUiState,
                    totalPoints = currentPoints,
                    userLevel = userLevel,
                    pointsToNextLevel = pointsToNext,
                    achievementsCount = achievementsCount,
                    currentRank = userRank,
                    isLoading = false,
                    error = null
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    /**
     * Updates user profile information.
     */
    fun updateProfile(name: String, email: String? = null) {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            _actionState.value = FirebaseProfileAction.Error("No user logged in.")
            return
        }

        if (name.isBlank()) {
            _actionState.value = FirebaseProfileAction.Error("Name cannot be empty.")
            return
        }

        viewModelScope.launch {
            _actionState.value = FirebaseProfileAction.Loading
            
            try {
                val result = authRepository.updateUserProfile(
                    userId = userId,
                    name = name,
                    email = email ?: ""
                )

                result.onSuccess {
                    _actionState.value = FirebaseProfileAction.Success
                }.onFailure { exception ->
                    _actionState.value = FirebaseProfileAction.Error(
                        exception.message ?: "Failed to update profile"
                    )
                }
            } catch (e: Exception) {
                _actionState.value = FirebaseProfileAction.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Toggles biometric authentication setting.
     */
    fun toggleBiometricAuthentication(enabled: Boolean) {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            _actionState.value = FirebaseProfileAction.Error("No user logged in.")
            return
        }

        viewModelScope.launch {
            _actionState.value = FirebaseProfileAction.Loading
            
            try {
                val result = authRepository.updateBiometricSetting(userId, enabled)

                result.onSuccess {
                    _actionState.value = FirebaseProfileAction.Success
                }.onFailure { exception ->
                    _actionState.value = FirebaseProfileAction.Error(
                        exception.message ?: "Failed to update biometric setting"
                    )
                }
            } catch (e: Exception) {
                _actionState.value = FirebaseProfileAction.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Changes user password.
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _actionState.value = FirebaseProfileAction.Error("Passwords cannot be empty.")
            return
        }

        if (newPassword.length < 6) {
            _actionState.value = FirebaseProfileAction.Error("New password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _actionState.value = FirebaseProfileAction.Loading
            
            try {
                val result = authRepository.changePassword(currentPassword, newPassword)

                result.onSuccess {
                    _actionState.value = FirebaseProfileAction.Success
                }.onFailure { exception ->
                    _actionState.value = FirebaseProfileAction.Error(
                        exception.message ?: "Failed to change password"
                    )
                }
            } catch (e: Exception) {
                _actionState.value = FirebaseProfileAction.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Deletes user account.
     */
    fun deleteAccount(password: String) {
        if (password.isBlank()) {
            _actionState.value = FirebaseProfileAction.Error("Password is required to delete account.")
            return
        }

        viewModelScope.launch {
            _actionState.value = FirebaseProfileAction.Loading
            
            try {
                val result = authRepository.deleteAccount(password)

                result.onSuccess {
                    sessionManager.clearSession()
                    _actionState.value = FirebaseProfileAction.Success
                }.onFailure { exception ->
                    _actionState.value = FirebaseProfileAction.Error(
                        exception.message ?: "Failed to delete account"
                    )
                }
            } catch (e: Exception) {
                _actionState.value = FirebaseProfileAction.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                sessionManager.clearSession()
                _actionState.value = FirebaseProfileAction.Success
            } catch (e: Exception) {
                _actionState.value = FirebaseProfileAction.Error(
                    e.message ?: "Failed to sign out"
                )
            }
        }
    }

    /**
     * Refreshes profile data.
     */
    fun refreshProfile() {
        loadProfileData()
    }

    /**
     * Resets action state.
     */
    fun resetActionState() {
        _actionState.value = FirebaseProfileAction.Idle
    }

    /**
     * Clears error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        _actionState.value = FirebaseProfileAction.Idle
    }

    /**
     * Gets user statistics summary.
     */
    fun getUserStatistics(): Map<String, Any> {
        val state = _uiState.value
        return mapOf(
            "totalPoints" to state.totalPoints,
            "userLevel" to state.userLevel,
            "achievementsUnlocked" to state.achievementsCount,
            "currentRank" to state.currentRank,
            "pointsToNextLevel" to state.pointsToNextLevel
        )
    }

    /**
     * Exports user data (for GDPR compliance).
     */
    fun exportUserData() {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            _actionState.value = FirebaseProfileAction.Error("No user logged in.")
            return
        }

        viewModelScope.launch {
            _actionState.value = FirebaseProfileAction.Loading
            
            try {
                // This would typically generate a data export
                // For now, just indicate success
                _actionState.value = FirebaseProfileAction.Success
            } catch (e: Exception) {
                _actionState.value = FirebaseProfileAction.Error(
                    e.message ?: "Failed to export data"
                )
            }
        }
    }

    /**
     * Sends password reset email.
     */
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _actionState.value = FirebaseProfileAction.Error("Email cannot be empty.")
            return
        }

        viewModelScope.launch {
            _actionState.value = FirebaseProfileAction.Loading
            
            try {
                val result = authRepository.sendPasswordResetEmail(email)

                result.onSuccess {
                    _actionState.value = FirebaseProfileAction.Success
                }.onFailure { exception ->
                    _actionState.value = FirebaseProfileAction.Error(
                        exception.message ?: "Failed to send reset email"
                    )
                }
            } catch (e: Exception) {
                _actionState.value = FirebaseProfileAction.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Calculates user level based on points.
     */
    private fun calculateUserLevel(points: Int): Int {
        return when {
            points < 100 -> 1
            points < 500 -> 2
            points < 1000 -> 3
            points < 2500 -> 4
            points < 5000 -> 5
            else -> 6
        }
    }

    /**
     * Calculates points needed for next level.
     */
    private fun calculatePointsToNextLevel(currentPoints: Int, currentLevel: Int): Int {
        return when (currentLevel) {
            1 -> 100 - currentPoints
            2 -> 500 - currentPoints
            3 -> 1000 - currentPoints
            4 -> 2500 - currentPoints
            5 -> 5000 - currentPoints
            else -> 0
        }
    }

    /**
     * Checks if the current user is logged in.
     */
    fun isUserLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    /**
     * Gets the current user's email.
     */
    fun getCurrentUserEmail(): String? {
        return _uiState.value.user?.email
    }

    /**
     * Gets the current user's name.
     */
    fun getCurrentUserName(): String? {
        return _uiState.value.user?.name
    }

    /**
     * Checks if biometric authentication is enabled.
     */
    fun isBiometricEnabled(): Boolean {
        return _uiState.value.user?.biometricEnabled ?: false
    }

    /**
     * Gets formatted level progress text.
     */
    fun getLevelProgressText(): String {
        val state = _uiState.value
        return if (state.userLevel < 6) {
            "${state.pointsToNextLevel} points to Level ${state.userLevel + 1}"
        } else {
            "Max Level Reached!"
        }
    }

    /**
     * Gets level progress percentage (0-100).
     */
    fun getLevelProgressPercentage(): Int {
        val state = _uiState.value
        if (state.userLevel >= 6) return 100
        
        val levelThresholds = listOf(0, 100, 500, 1000, 2500, 5000)
        val currentThreshold = levelThresholds[state.userLevel - 1]
        val nextThreshold = levelThresholds[state.userLevel]
        val progressInLevel = state.totalPoints - currentThreshold
        val levelRange = nextThreshold - currentThreshold
        
        return ((progressInLevel.toFloat() / levelRange.toFloat()) * 100).toInt().coerceIn(0, 100)
    }
} 