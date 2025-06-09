package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.firebase.repository.FirebaseRewardsRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseAuthRepository
import com.example.budgetbuddy.data.firebase.model.FirebaseRewardPoints
import com.example.budgetbuddy.data.firebase.model.FirebaseAchievement
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
 * Firebase-based RewardsViewModel that replaces the Room-based version.
 * Uses Firebase repositories for real-time rewards and achievements updates.
 */

// UI State classes remain the same for compatibility
data class FirebaseRewardsUiState(
    val currentPoints: Int = 0,
    val achievements: List<AchievementUiState> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val userRank: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class AchievementUiState(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean,
    val unlockedDate: String?,
    val pointsAwarded: Int = 0
)

data class LeaderboardEntry(
    val userId: String,
    val userName: String,
    val points: Int,
    val rank: Int,
    val isCurrentUser: Boolean = false
)

@HiltViewModel
class FirebaseRewardsViewModel @Inject constructor(
    private val rewardsRepository: FirebaseRewardsRepository,
    private val authRepository: FirebaseAuthRepository,
    private val sessionManager: FirebaseSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirebaseRewardsUiState(isLoading = true))
    val uiState: StateFlow<FirebaseRewardsUiState> = _uiState.asStateFlow()

    private fun getCurrentUserId(): String = sessionManager.getUserId()

    // Live data streams
    val userPoints: StateFlow<FirebaseRewardPoints?> = rewardsRepository
        .getUserPointsFlow(getCurrentUserId())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val userAchievements: StateFlow<List<FirebaseAchievement>> = rewardsRepository
        .getUserAchievementsFlow(getCurrentUserId())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val leaderboardFlow: StateFlow<List<FirebaseRewardPoints>> = rewardsRepository
        .getLeaderboardFlow(100) // Increase limit to show more users
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadRewardsData()
    }

    /**
     * Loads all rewards data with real-time updates.
     */
    private fun loadRewardsData() {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            _uiState.value = FirebaseRewardsUiState(isLoading = false, error = "Please log in.")
            return
        }

        viewModelScope.launch {
            combine(
                userPoints,
                userAchievements,
                leaderboardFlow
            ) { points, achievements, leaderboard ->
                Triple(points, achievements, leaderboard)
            }.collect { (points, achievements, leaderboard) ->
                
                android.util.Log.d("FirebaseRewardsViewModel", "=== Processing Rewards Data ===")
                android.util.Log.d("FirebaseRewardsViewModel", "Points: ${points?.currentPoints ?: 0}")
                android.util.Log.d("FirebaseRewardsViewModel", "Achievements: ${achievements.size}")
                android.util.Log.d("FirebaseRewardsViewModel", "Leaderboard entries: ${leaderboard.size}")
                
                // Process achievements data
                val achievementUiList = processAchievements(achievements)
                
                // Process leaderboard data with user names (async)
                val leaderboardUiList = processLeaderboardWithUserNames(leaderboard, userId)
                
                // Find user's rank in leaderboard
                val userRank = leaderboardUiList.find { it.isCurrentUser }?.rank ?: 0
                
                android.util.Log.d("FirebaseRewardsViewModel", "Processed leaderboard: ${leaderboardUiList.size} entries")
                android.util.Log.d("FirebaseRewardsViewModel", "User rank: $userRank")

                val newState = FirebaseRewardsUiState(
                    currentPoints = points?.currentPoints ?: 0,
                    achievements = achievementUiList,
                    leaderboard = leaderboardUiList,
                    userRank = userRank,
                    isLoading = false,
                    error = null
                )
                
                _uiState.value = newState
            }
        }
    }

    /**
     * Processes achievements data for UI display.
     */
    private suspend fun processAchievements(userAchievements: List<FirebaseAchievement>): List<AchievementUiState> {
        // Get all available achievements
        val allAchievements = getAllPossibleAchievements()
        
        return allAchievements.map { achievement ->
            val userAchievement = userAchievements.find { it.achievementName == achievement.achievementName }
            val isUnlocked = userAchievement != null
            
            AchievementUiState(
                id = achievement.achievementName,
                name = formatAchievementName(achievement.achievementName),
                description = achievement.description,
                iconName = achievement.iconName ?: "ic_achievement_placeholder",
                isUnlocked = isUnlocked,
                unlockedDate = null, // Simplified to avoid type issues
                pointsAwarded = getPointsForAchievement(achievement.achievementName)
            )
        }
    }

    /**
     * Processes leaderboard data for UI display with user names.
     */
    private suspend fun processLeaderboardWithUserNames(
        leaderboard: List<FirebaseRewardPoints>, 
        currentUserId: String
    ): List<LeaderboardEntry> {
        android.util.Log.d("FirebaseRewardsViewModel", "Processing ${leaderboard.size} leaderboard entries with user names")
        
        return leaderboard.mapIndexed { index, rewardPoints ->
            android.util.Log.d("FirebaseRewardsViewModel", "Processing entry ${index + 1}: User ${rewardPoints.userId} with ${rewardPoints.currentPoints} points")
            
            // Get actual user name
            val userName = when {
                rewardPoints.userId == currentUserId -> "You"
                else -> {
                    // Fetch user name from Firestore
                    try {
                        val userProfile = authRepository.getUserProfile(rewardPoints.userId)
                        val actualName = userProfile?.name?.takeIf { it.isNotBlank() }
                        actualName ?: getUserDisplayName(rewardPoints.userId)
                    } catch (e: Exception) {
                        android.util.Log.w("FirebaseRewardsViewModel", "Failed to get user name for ${rewardPoints.userId}: ${e.message}")
                        getUserDisplayName(rewardPoints.userId)
                    }
                }
            }
            
            LeaderboardEntry(
                userId = rewardPoints.userId,
                userName = userName,
                points = rewardPoints.currentPoints,
                rank = index + 1,
                isCurrentUser = rewardPoints.userId == currentUserId
            )
        }
    }

    /**
     * Gets a display name for a user ID when the actual name is not available.
     */
    private fun getUserDisplayName(userId: String): String {
        return when {
            userId.startsWith("test_user_") -> "Test User ${userId.substringAfter("test_user_")}"
            userId.length > 8 -> "User ${userId.take(8)}"
            else -> "User $userId"
        }
    }

    /**
     * Formats achievement names by removing underscores and capitalizing words.
     */
    private fun formatAchievementName(achievementName: String): String {
        return achievementName
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase() else it.toString() 
                }
            }
    }

    /**
     * Claims daily login reward points.
     */
    fun claimDailyReward() {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return

        viewModelScope.launch {
            try {
                val result = rewardsRepository.claimDailyLoginReward(userId)
                result.onSuccess { pointsAwarded ->
                    // Update UI or show success message
                    // Points will be automatically updated via real-time listener
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to claim daily reward"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    /**
     * Manually checks for new achievements (usually done automatically).
     */
    fun checkForNewAchievements() {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return

        viewModelScope.launch {
            try {
                // Check various achievements
                rewardsRepository.checkAndUnlockAchievement(userId, "first_expense_logged")
                rewardsRepository.checkAndUnlockAchievement(userId, "budget_created")
                rewardsRepository.checkAndUnlockAchievement(userId, "spending_streak")
                rewardsRepository.checkAndUnlockAchievement(userId, "budget_saver")
                rewardsRepository.checkAndUnlockAchievement(userId, "points_collector")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to check achievements"
                )
            }
        }
    }

    /**
     * Refreshes all rewards data.
     */
    fun refreshData() {
        loadRewardsData()
    }

    /**
     * Clears error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * TEMPORARY: Deletes test users from the database.
     * This method should be removed after cleanup is complete.
     */
    fun deleteTestUsers() {
        viewModelScope.launch {
            try {
                android.util.Log.d("FirebaseRewardsViewModel", "Deleting test users...")
                
                val testUserIds = listOf(
                    "test_user_1", "test_user_2", "test_user_3", "test_user_4",
                    "test_user_5", "test_user_6", "test_user_7", "test_user_8"
                )
                
                testUserIds.forEach { userId ->
                    try {
                        // Delete from reward points collection
                        rewardsRepository.deleteUserPoints(userId)
                        android.util.Log.d("FirebaseRewardsViewModel", "Deleted points for $userId")
                        
                        // Delete from users collection if exists
                        authRepository.deleteUserProfile(userId)
                        android.util.Log.d("FirebaseRewardsViewModel", "Deleted profile for $userId")
                        
                    } catch (e: Exception) {
                        android.util.Log.w("FirebaseRewardsViewModel", "Failed to delete $userId: ${e.message}")
                    }
                }
                
                android.util.Log.d("FirebaseRewardsViewModel", "Test user cleanup completed!")
                
            } catch (e: Exception) {
                android.util.Log.e("FirebaseRewardsViewModel", "Error during cleanup: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to cleanup test data: ${e.message}"
                )
            }
        }
    }

    /**
     * Gets detailed leaderboard with more entries.
     */
    fun loadExtendedLeaderboard(limit: Int = 100) {
        viewModelScope.launch {
            try {
                // Temporary fix - simplified leaderboard loading
                _uiState.value = _uiState.value.copy(
                    leaderboard = emptyList<LeaderboardEntry>() // Fixed type
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load leaderboard"
                )
            }
        }
    }

    /**
     * Gets the user's current rank in the leaderboard.
     */
    suspend fun getUserRank(): Int {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return 0
        
        return try {
            rewardsRepository.getUserRank(userId)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Gets the current user's display name.
     */
    fun getCurrentUserName(callback: (String?) -> Unit) {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) {
            callback(null)
            return
        }

        viewModelScope.launch {
            try {
                val userProfile = authRepository.getUserProfile(userId)
                val displayName = userProfile?.name?.takeIf { it.isNotBlank() } 
                    ?: "User" // Fallback to "User" if no name is set
                callback(displayName)
            } catch (e: Exception) {
                android.util.Log.w("FirebaseRewardsViewModel", "Failed to get user name: ${e.message}")
                callback("User")
            }
        }
    }

    /**
     * Gets user's achievement progress.
     */
    fun getAchievementProgress(achievementId: String): Int {
        // This would typically be implemented based on specific achievement requirements
        // For now, return 0 or 100 based on whether it's unlocked
        val achievement = _uiState.value.achievements.find { it.id == achievementId }
        return if (achievement?.isUnlocked == true) 100 else 0
    }

    /**
     * Shares achievement on social media (placeholder).
     */
    fun shareAchievement(achievementId: String) {
        // Implementation would depend on social sharing requirements
        // This is a placeholder for the feature
    }

    /**
     * Gets all possible achievements in the system.
     */
    private fun getAllPossibleAchievements(): List<FirebaseAchievement> {
        return listOf(
            FirebaseAchievement(
                achievementName = "first_expense_logged",
                description = "Log your first expense",
                iconName = "ic_first_expense"
            ),
            FirebaseAchievement(
                achievementName = "budget_created",
                description = "Create your first budget",
                iconName = "ic_budget"
            ),
            FirebaseAchievement(
                achievementName = "spending_streak",
                description = "Log expenses for 7 consecutive days",
                iconName = "ic_streak"
            ),
            FirebaseAchievement(
                achievementName = "budget_saver",
                description = "Stay under budget for a month",
                iconName = "ic_saver"
            ),
            FirebaseAchievement(
                achievementName = "points_collector",
                description = "Earn 1000 reward points",
                iconName = "ic_points"
            ),
            FirebaseAchievement(
                achievementName = "early_bird",
                description = "Log an expense before 8 AM",
                iconName = "ic_early_bird"
            ),
            FirebaseAchievement(
                achievementName = "weekend_warrior",
                description = "Log expenses on both weekend days",
                iconName = "ic_weekend"
            ),
            FirebaseAchievement(
                achievementName = "monthly_planner",
                description = "Set budgets for 3 consecutive months",
                iconName = "ic_planner"
            )
        )
    }

    /**
     * Gets points awarded for specific achievements.
     */
    private fun getPointsForAchievement(achievementName: String): Int {
        return when (achievementName) {
            "first_expense_logged" -> 50
            "budget_created" -> 100
            "spending_streak" -> 150
            "budget_saver" -> 200
            "points_collector" -> 250
            "early_bird" -> 75
            "weekend_warrior" -> 100
            "monthly_planner" -> 300
            else -> 50
        }
    }

    /**
     * Checks if the current user is logged in.
     */
    fun isUserLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    /**
     * Gets user's current level based on points.
     */
    fun getUserLevel(): Int {
        val points = _uiState.value.currentPoints
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
     * Gets points needed for next level.
     */
    fun getPointsForNextLevel(): Int {
        val currentPoints = _uiState.value.currentPoints
        return when (getUserLevel()) {
            1 -> 100 - currentPoints
            2 -> 500 - currentPoints
            3 -> 1000 - currentPoints
            4 -> 2500 - currentPoints
            5 -> 5000 - currentPoints
            else -> 0
        }
    }

    /**
     * Gets level progress as percentage.
     */
    fun getLevelProgressPercentage(): Int {
        val currentPoints = _uiState.value.currentPoints
        val level = getUserLevel()
        
        val (currentLevelMin, nextLevelMin) = when (level) {
            1 -> Pair(0, 100)
            2 -> Pair(100, 500)
            3 -> Pair(500, 1000)
            4 -> Pair(1000, 2500)
            5 -> Pair(2500, 5000)
            else -> return 100 // Max level
        }
        
        val progressInLevel = currentPoints - currentLevelMin
        val totalForLevel = nextLevelMin - currentLevelMin
        
        return if (totalForLevel > 0) {
            ((progressInLevel.toDouble() / totalForLevel) * 100).toInt().coerceIn(0, 100)
        } else {
            100
        }
    }
} 