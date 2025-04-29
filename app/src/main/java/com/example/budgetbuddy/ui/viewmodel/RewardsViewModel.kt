package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.R
import com.example.budgetbuddy.model.Badge // Assuming model exists
import com.example.budgetbuddy.model.LeaderboardRank // Assuming model exists
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.budgetbuddy.data.repository.UserRepository
import com.example.budgetbuddy.data.repository.RewardsRepository
import com.example.budgetbuddy.data.db.entity.UserEntity
import com.example.budgetbuddy.data.db.entity.AchievementEntity
import com.example.budgetbuddy.data.db.entity.RewardPointsEntity
import kotlinx.coroutines.flow.combine // Needed to combine flows
import kotlin.math.min

data class RewardsUiState(
    val userName: String = "",
    val userLevel: String = "",
    val userProfileImageUrl: String? = null, // Or use a placeholder drawable ID
    val nextRewardName: String = "",
    val nextRewardProgress: Int = 0, // 0-100
    val badges: List<Badge> = emptyList(),
    val leaderboard: List<LeaderboardRank> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val rewardsRepository: RewardsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    // TODO: Replace with actual user ID retrieval
    private val currentUserId: Long = 1L

    init {
        loadRewardsData()
    }

    private fun loadRewardsData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Combine flows for user data, points, and achievements
                combine(
                    userRepository.getUser(currentUserId),
                    rewardsRepository.getPoints(currentUserId),
                    rewardsRepository.getAchievements(currentUserId)
                ) { user, pointsData, achievements ->

                    // Process User Info
                    val userName = user?.name ?: "User"
                    // Process Level & Progress (Example Logic)
                    val currentPoints = pointsData?.currentPoints ?: 0
                    val (levelData, nextRewardName) = calculateLevelAndProgress(currentPoints)
                    val (level, levelName, progress) = levelData
                    val userLevelText = "Level $level $levelName"

                    // Process Badges (Map Achievements to Badges)
                    val badges = mapAchievementsToBadges(achievements)

                    // Leaderboard (Still Placeholder)
                    val placeholderLeaderboard = getPlaceholderLeaderboard(user?.name ?: "You")

                    // Update State
                    RewardsUiState(
                        isLoading = false,
                        userName = userName,
                        userLevel = userLevelText,
                        userProfileImageUrl = null, // TODO: Add profile image URL to UserEntity if needed
                        nextRewardName = nextRewardName,
                        nextRewardProgress = progress,
                        badges = badges,
                        leaderboard = placeholderLeaderboard,
                        error = null
                    )
                }.collect { newState ->
                     _uiState.value = newState
                }
            } catch (e: Exception) {
                 _uiState.update { it.copy(isLoading = false, error = "Failed to load rewards data") }
            }
        }
    }

    // Example level calculation logic (customize as needed)
    // Returns a Pair: Triple(Level, LevelName, ProgressPercent) and NextRewardName String
    private fun calculateLevelAndProgress(points: Int): Pair<Triple<Int, String, Int>, String> {
        val pointsPerLevel = 500 // Example
        val level = (points / pointsPerLevel) + 1
        val pointsInLevel = points % pointsPerLevel
        val progressPercent = ((pointsInLevel.toDouble() / pointsPerLevel) * 100).toInt().coerceIn(0, 100)
        
        // Determine level name and next reward based on level
        val levelName = when (level) {
            1 -> "Beginner"
            in 2..4 -> "Budgeter"
            in 5..9 -> "Saver"
            in 10..19 -> "Master"
            else -> "Guru"
        }
        val nextReward = "Reach Level ${level + 1}"
        
        return Pair(Triple(level, levelName, progressPercent), nextReward)
    }

    // Map AchievementEntity to Badge model
    private fun mapAchievementsToBadges(achievements: List<AchievementEntity>): List<Badge> {
        return achievements.filter { it.achievedDate != null } // Only show unlocked badges
            .map { achievement ->
                Badge(
                    id = achievement.achievementId.toString(),
                    name = achievement.achievementName, 
                    iconResId = getIconForAchievement(achievement.achievementName) // Map name/id to icon
                )
            }
    }

    // Placeholder icon mapping for badges
    private fun getIconForAchievement(achievementName: String): Int {
        // Basic example - expand this logic
        return when {
            achievementName.contains("Budget") -> R.drawable.ic_trophy
            achievementName.contains("Expense") -> R.drawable.ic_track_expenses
            achievementName.contains("Saving") -> R.drawable.ic_achievement_unlocked
            else -> R.drawable.ic_set_goals
        }
    }

    // Placeholder Leaderboard
    private fun getPlaceholderLeaderboard(currentUserName: String): List<LeaderboardRank> {
         return listOf(
                LeaderboardRank("user1", "Sarah Miller", R.drawable.ic_profile_placeholder, 2450),
                LeaderboardRank("user2", "John Doe", R.drawable.ic_profile_placeholder, 2280),
                LeaderboardRank(currentUserId.toString(), currentUserName, R.drawable.ic_profile_placeholder, 2150) // Example placement
            ).sortedByDescending { it.points }
    }
} 