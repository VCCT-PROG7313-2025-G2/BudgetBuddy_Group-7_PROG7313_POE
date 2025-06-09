package com.example.budgetbuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetbuddy.data.firebase.repository.FirebaseRewardsRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseAuthRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseBudgetRepository
import com.example.budgetbuddy.data.firebase.repository.FirebaseExpenseRepository
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar

/**
 * Firebase-based RewardsViewModel that replaces the Room-based version.
 * Uses Firebase repositories for real-time rewards and achievements updates.
 */

// UI State classes remain the same for compatibility
data class PerformanceSummary(
    val budgetPerformancePercentage: Int = 0,
    val budgetScore: String = "N/A",
    val pointsThisMonth: Int = 0,
    val pointsTrend: String = "→",
    val overallGrade: String = "N/A",
    val overallScore: Int = 0,
    val performanceMessage: String = "No data available"
)

data class FirebaseRewardsUiState(
    val currentPoints: Int = 0,
    val achievements: List<AchievementUiState> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val userRank: Int = 0,
    val performanceSummary: PerformanceSummary = PerformanceSummary(),
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
    private val budgetRepository: FirebaseBudgetRepository,
    private val expenseRepository: FirebaseExpenseRepository,
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
                
                // Calculate performance summary
                val performanceSummary = calculatePerformanceSummary(points?.currentPoints ?: 0)
                
                android.util.Log.d("FirebaseRewardsViewModel", "Processed leaderboard: ${leaderboardUiList.size} entries")
                android.util.Log.d("FirebaseRewardsViewModel", "User rank: $userRank")

                val newState = FirebaseRewardsUiState(
                    currentPoints = points?.currentPoints ?: 0,
                    achievements = achievementUiList,
                    leaderboard = leaderboardUiList,
                    userRank = userRank,
                    performanceSummary = performanceSummary,
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
     * Gets current user's profile image URL.
     */
    fun getCurrentUserProfileImage(callback: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId.isNotEmpty()) {
                    val userProfile = authRepository.getUserProfile(userId)
                    callback(userProfile?.profileImageUrl)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseRewardsViewModel", "Error loading user profile image", e)
                callback(null)
            }
        }
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

    /**
     * Calculates performance summary based on number of exceeded budget categories and points earned.
     */
    private suspend fun calculatePerformanceSummary(currentPoints: Int): PerformanceSummary {
        val userId = getCurrentUserId()
        val currentDate = Date()
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentDate)
        
        return try {
            // Get current month budget and category budgets
            val budget = budgetRepository.getBudgetForMonthDirect(userId, monthYear)
            
            if (budget == null) {
                // No budget set, give default performance
                return PerformanceSummary(
                    budgetPerformancePercentage = 50,
                    budgetScore = "C",
                    pointsThisMonth = currentPoints,
                    pointsTrend = "→",
                    overallGrade = "C",
                    overallScore = 50,
                    performanceMessage = "Set up a budget to track your performance"
                )
            }
            
            // Get category budgets and spending data
            val categoryBudgets = budgetRepository.getCategoryBudgetsForBudgetDirect(budget.id)
            val startDate = getStartOfMonth(monthYear)
            val endDate = getEndOfMonth(monthYear)
            val categorySpending = expenseRepository.getSpendingByCategoryBetween(userId, startDate, endDate)
            
            // Count exceeded budget categories
            var exceededCategories = 0
            categoryBudgets.forEach { categoryBudget ->
                val spent = categorySpending[categoryBudget.categoryName] ?: BigDecimal.ZERO
                val allocated = categoryBudget.getAllocatedAmountAsBigDecimal()
                
                if (allocated > BigDecimal.ZERO && spent > allocated) {
                    exceededCategories++
                    android.util.Log.d("RewardsViewModel", "Exceeded category: ${categoryBudget.categoryName} - Spent: $spent, Budget: $allocated")
                }
            }
            
            android.util.Log.d("RewardsViewModel", "Total exceeded categories: $exceededCategories out of ${categoryBudgets.size}")
            
            // Calculate budget score based on exceeded categories
            val (budgetScore, budgetPerformancePercentage) = when (exceededCategories) {
                0 -> Pair("A", 100)  // No budgets exceeded = A grade
                1 -> Pair("B", 85)   // 1 budget exceeded = B grade
                in 2..4 -> Pair("C", 70)  // 2-4 budgets exceeded = C grade
                else -> Pair("F", 25)     // 4+ budgets exceeded = F grade
            }
            
            // Estimate points earned this month (simplified - you might want to track this more precisely)
            val pointsThisMonth = currentPoints // For now, use current points as monthly estimate
            
            // Determine points trend (simplified - comparing to a baseline)
            val pointsTrend = when {
                pointsThisMonth >= 200 -> "↗"  // Rising
                pointsThisMonth >= 100 -> "→"  // Stable
                else -> "↘"  // Falling
            }
            
            // Calculate overall performance (average of budget performance and points factor)
            val pointsFactor = when {
                pointsThisMonth >= 300 -> 100
                pointsThisMonth >= 200 -> 80
                pointsThisMonth >= 100 -> 60
                pointsThisMonth >= 50 -> 40
                else -> 20
            }
            
            val overallScore = ((budgetPerformancePercentage + pointsFactor) / 2)
            
            val overallGrade = when (overallScore) {
                in 90..100 -> "A"
                in 80..89 -> "B"
                in 70..79 -> "C"
                in 60..69 -> "D"
                else -> "F"
            }
            
            val performanceMessage = when (overallGrade) {
                "A" -> "Excellent! You're managing your finances brilliantly"
                "B" -> "Great job! You're on track with your financial goals"
                "C" -> "Good progress, but there's room for improvement"
                "D" -> "You're making progress, keep working on your budget"
                "F" -> "Let's focus on getting back on track with your budget"
                else -> "Keep up the good work!"
            }
            
            PerformanceSummary(
                budgetPerformancePercentage = budgetPerformancePercentage,
                budgetScore = budgetScore,
                pointsThisMonth = pointsThisMonth,
                pointsTrend = pointsTrend,
                overallGrade = overallGrade,
                overallScore = overallScore,
                performanceMessage = performanceMessage
            )
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRewardsViewModel", "Error calculating performance summary", e)
            PerformanceSummary(
                performanceMessage = "Unable to calculate performance data"
            )
        }
    }
    
    /**
     * Helper to get start of month date.
     */
    private fun getStartOfMonth(monthYear: String): Date {
        val parts = monthYear.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1 // Calendar months are 0-based
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    /**
     * Helper to get end of month date.
     */
    private fun getEndOfMonth(monthYear: String): Date {
        val parts = monthYear.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1 // Calendar months are 0-based
        
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        return calendar.time
    }
} 