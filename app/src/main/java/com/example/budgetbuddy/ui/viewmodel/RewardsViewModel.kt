package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.R
import com.example.budgetbuddy.model.Badge
import com.example.budgetbuddy.model.LeaderboardRank
import com.example.budgetbuddy.model.UserWithPoints
import com.example.budgetbuddy.data.db.entity.AchievementEntity
import com.example.budgetbuddy.data.repository.RewardsRepository
import com.example.budgetbuddy.data.repository.UserRepository
import com.example.budgetbuddy.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

// Data class for holding level calculation results
data class LevelProgressInfo(
    val level: Int,
    val levelName: String,
    val pointsInLevel: Int,
    val pointsNeededForLevel: Int,
    val nextLevelThreshold: Int // Points needed to reach next level
)

data class RewardsUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val currentPoints: Int = 0,
    val userLevel: Int = 0,
    val userLevelName: String = "",
    val pointsInLevel: Int = 0,
    val pointsNeededForLevel: Int = 1,
    val nextLevelPoints: Int = 0,
    val achievements: List<Badge> = emptyList(), // Use Badge model
    val leaderboardEntries: List<LeaderboardRank> = emptyList(),
    val error: String? = null
    // Removed redundant fields like userProfileImageUrl, nextRewardName if handled differently
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val rewardsRepository: RewardsRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState()) // Use initial state from data class
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    private fun getCurrentUserId(): Long = sessionManager.getUserId()

    init {
        loadRewardsData()
    }

    private fun loadRewardsData() {
        val userId = getCurrentUserId()
        if (userId == SessionManager.NO_USER_LOGGED_IN) {
            Log.e("RewardsViewModel", "Cannot load data, no user logged in")
            _uiState.update { it.copy(isLoading = false, error = "Please log in to see rewards.") }
            return
        }

        viewModelScope.launch {
            try {
                // Use correct method name from UserRepository
                // Combine user, points, achievements, and leaderboard flows
                combine(
                    userRepository.getUser(userId),           // Correct method
                    rewardsRepository.getPoints(userId),
                    rewardsRepository.getAchievements(userId),
                    rewardsRepository.getLeaderboard()
                ) { user, pointsEntity, achievements, leaderboard ->
                    // Process the combined data
                    val currentPoints = pointsEntity?.currentPoints ?: 0
                    val levelInfo = calculateLevelAndProgress(currentPoints) // Calculate level info
                    val userBadges = mapAchievementsToBadges(achievements) // Map achievements to badges
                    val leaderboardItems = mapLeaderboardEntries(leaderboard) // Map leaderboard data

                    // Update the UI State
                    RewardsUiState(
                        isLoading = false,
                        userName = user?.name ?: "User",
                        currentPoints = currentPoints,
                        userLevel = levelInfo.level,
                        userLevelName = levelInfo.levelName,
                        pointsInLevel = levelInfo.pointsInLevel,
                        pointsNeededForLevel = levelInfo.pointsNeededForLevel,
                        nextLevelPoints = levelInfo.nextLevelThreshold,
                        achievements = userBadges, // Use the mapped badges
                        leaderboardEntries = leaderboardItems, // Use the mapped leaderboard items
                        error = null
                    )
                }.catch { e ->
                    Log.e("RewardsViewModel", "Error loading rewards data for user $userId", e)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load rewards data.") }
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                Log.e("RewardsViewModel", "Error in loadRewardsData launch block", e)
                _uiState.update { it.copy(isLoading = false, error = "An unexpected error occurred.") }
            }
        }
    }

    // Moved level calculation logic directly into ViewModel for simplicity
    private fun calculateLevelAndProgress(points: Int): LevelProgressInfo {
        // Define new thresholds based on user request
        val thresholds = listOf(0, 150, 450, 1050, 2250)
        val names = listOf("Bronze", "Silver", "Gold", "Platinum", "Diamond") // Keep names or adjust if needed

        var level = 1
        var levelName = names[0]
        var pointsForCurrentLevelStart = thresholds[0]
        var pointsForNextLevelStart = thresholds.getOrElse(1) { thresholds.last() + 1000 } // Default next if only one level

        for (i in thresholds.indices) {
            if (points >= thresholds[i]) {
                level = i + 1
                levelName = names.getOrElse(i) { names.last() }
                pointsForCurrentLevelStart = thresholds[i]
                pointsForNextLevelStart = thresholds.getOrElse(i + 1) { points + 1 } // If max level, next threshold is just above current points
            } else {
                pointsForNextLevelStart = thresholds[i] // The threshold we didn't reach
                break
            }
        }

        val pointsNeededForLevel = (pointsForNextLevelStart - pointsForCurrentLevelStart).coerceAtLeast(1)
        val pointsInLevel = (points - pointsForCurrentLevelStart).coerceIn(0, pointsNeededForLevel)

        return LevelProgressInfo(
            level = level,
            levelName = levelName,
            pointsInLevel = pointsInLevel,
            pointsNeededForLevel = pointsNeededForLevel,
            nextLevelThreshold = pointsForNextLevelStart // Total points needed for next level
        )
    }

    // Map AchievementEntity list to Badge list
    private fun mapAchievementsToBadges(achievements: List<AchievementEntity>): List<Badge> {
        return achievements
            .filter { it.achievedDate != null } // Only show unlocked achievements as badges
            .map { achievement ->
                Badge(
                    id = achievement.achievementId.toString(),
                    name = achievement.achievementName,
                    iconResId = getIconForAchievement(achievement.iconName) // Use iconName from entity
                )
            }
    }

    // Placeholder icon mapping - use iconName from AchievementEntity
    private fun getIconForAchievement(iconName: String?): Int {
        return when (iconName) {
            "ic_track_expenses" -> R.drawable.ic_track_expenses
            "ic_set_goals" -> R.drawable.ic_set_goals
            "ic_trophy" -> R.drawable.ic_trophy
            // Add mappings for other icon names stored in the DB
            else -> R.drawable.ic_achievement_unlocked // Default fallback icon
        }
    }

    // Map UserWithPoints list to LeaderboardRank list
    private fun mapLeaderboardEntries(usersWithPoints: List<UserWithPoints>): List<LeaderboardRank> {
        return usersWithPoints.mapIndexed { index, userWithPoints -> // Add index for rank
            LeaderboardRank(
                // rank = index + 1, // Rank is based on position
                userId = userWithPoints.user.userId.toString(),
                name = userWithPoints.user.name ?: "Unknown User",
                profileImageRes = R.drawable.ic_profile_placeholder, // TODO: Use real image if available
                points = userWithPoints.points.currentPoints
            )
        }
    }

    fun refreshData() {
        loadRewardsData()
    }
} 