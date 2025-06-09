package com.example.budgetbuddy.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.budgetbuddy.R
import com.example.budgetbuddy.adapter.BadgeAdapter
import com.example.budgetbuddy.adapter.LeaderboardAdapter
import com.example.budgetbuddy.databinding.FragmentRewardsBinding
import com.example.budgetbuddy.ui.viewmodel.FirebaseRewardsViewModel
import com.example.budgetbuddy.ui.viewmodel.PerformanceSummary
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.view.isVisible
import androidx.appcompat.app.AppCompatActivity

@AndroidEntryPoint
class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FirebaseRewardsViewModel by viewModels()
    private lateinit var badgeAdapter: BadgeAdapter
    private lateinit var leaderboardAdapter: LeaderboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        // Badge Adapter
        badgeAdapter = BadgeAdapter(emptyList()) // Assuming adapter takes List<Badge>
        binding.badgesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = badgeAdapter
        }

        // Leaderboard Adapter
        leaderboardAdapter = LeaderboardAdapter(emptyList()) // Assuming adapter takes List<LeaderboardRank>
        binding.leaderboardRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = leaderboardAdapter
            isNestedScrollingEnabled = false // Important for RecyclerView inside NestedScrollView
        }
    }

    private fun setupClickListeners() {
        binding.shareButton.setOnClickListener {
            shareRewards()
        }
        
        // TEMPORARY: Long-press share button to delete test users
        binding.shareButton.setOnLongClickListener {
            viewModel.deleteTestUsers()
            Toast.makeText(context, "Deleting test users...", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun shareRewards() {
        val state = viewModel.uiState.value // Get the current state
        val points = state.currentPoints   // Use correct field name
        val level = viewModel.getUserLevel() // Get level from ViewModel method

        // Construct the share message
        val shareText = "Check out my progress on BudgetBuddy! I\'m level $level with $points points. Join me in managing finances! #BudgetBuddyApp"
        // TODO: Consider adding a dynamic link to the app store or website

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        try {
            startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share content. No sharing app found?", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Handle Loading State
                    binding.loadingProgressBar.isVisible = state.isLoading
                    binding.mainContentGroup.isVisible = !state.isLoading

                    // Update UI only when not loading
                    if (!state.isLoading) {
                        // Update User Info - Get actual user name and profile image
                        viewModel.getCurrentUserName { userName ->
                            binding.userNameTextView.text = userName ?: "User"
                        }
                        
                        viewModel.getCurrentUserProfileImage { profileImageUrl ->
                            Glide.with(this@RewardsFragment)
                                .load(profileImageUrl ?: R.drawable.ic_profile_placeholder)
                                .transform(CircleCrop())
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .into(binding.userProfileImageView)
                        }
                        
                        // Combine level number
                        val userLevel = viewModel.getUserLevel()
                        binding.userLevelTextView.text = "Level $userLevel"

                        // Update Next Reward section based on points/level
                        val pointsToNext = viewModel.getPointsForNextLevel()
                        binding.nextRewardNameTextView.text = if (pointsToNext > 0) {
                            "Next Level: $pointsToNext pts needed"
                        } else {
                            "Max Level Reached!"
                        }
                        
                        // Set progress bar for level progress
                        if (pointsToNext > 0) {
                            val levelProgress = viewModel.getLevelProgressPercentage()
                            binding.nextRewardProgressBar.max = 100
                            binding.nextRewardProgressBar.progress = levelProgress
                            binding.nextRewardPercentageTextView.text = "$levelProgress%"
                        } else {
                            binding.nextRewardProgressBar.max = 100
                            binding.nextRewardProgressBar.progress = 100
                            binding.nextRewardPercentageTextView.text = "100%"
                        }

                        // *** Add Logging Here ***
                        Log.d("RewardsFragment", "Updating badge adapter. Badge count: ${state.achievements.size}")
                        state.achievements.forEach { badge ->
                             Log.d("RewardsFragment", "  - Badge: ${badge.name}, ID: ${badge.id}")
                        }
                        // ***********************

                        // Update Badges RecyclerView using state.achievements
                        // Note: Need to convert FirebaseAchievementUiState to Badge model
                        val badges = state.achievements.map { achievement ->
                            com.example.budgetbuddy.model.Badge(
                                id = achievement.id,
                                name = achievement.name,
                                iconResId = R.drawable.ic_achievement_placeholder // Use placeholder
                            )
                        }
                        badgeAdapter.updateData(badges)

                        // *** Add Logging Here Too ***
                        Log.d("RewardsFragment", "Updating leaderboard adapter. Entry count: ${state.leaderboard.size}")
                        state.leaderboard.forEachIndexed { index, entry ->
                            Log.d("RewardsFragment", "  - Rank ${index + 1}: ${entry.userName} with ${entry.points} points (User ID: ${entry.userId})")
                        }
                        // **************************

                        // Update Leaderboard RecyclerView using state.leaderboard
                        // Note: Need to convert LeaderboardEntry to LeaderboardRank model
                        val leaderboardRanks = state.leaderboard.map { entry ->
                            com.example.budgetbuddy.model.LeaderboardRank(
                                userId = entry.userId,
                                name = entry.userName,
                                points = entry.points,
                                profileImageRes = R.drawable.ic_profile_placeholder
                            )
                        }
                        leaderboardAdapter.updateData(leaderboardRanks)
                        
                        Log.d("RewardsFragment", "Leaderboard adapter updated with ${leaderboardRanks.size} entries")
                        
                        // Update Performance Summary
                        updatePerformanceSummary(state.performanceSummary)
                    }

                    // Handle error state (can be shown even if loading fails)
                    state.error?.let {
                        Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
                        // Optionally hide loading indicator on error too
                        binding.loadingProgressBar.isVisible = false
                    }
                }
            }
        }
    }

    private fun updatePerformanceSummary(summary: PerformanceSummary) {
        // Update budget performance
        binding.budgetPerformanceTextView.text = when {
            summary.budgetPerformancePercentage >= 90 -> "Excellent budget management this month"
            summary.budgetPerformancePercentage >= 75 -> "Good budget control this month"
            summary.budgetPerformancePercentage >= 50 -> "Fair budget management this month"
            summary.budgetPerformancePercentage >= 25 -> "Budget needs attention this month"
            else -> "Focus on budget management this month"
        }
        
        binding.budgetScoreTextView.text = summary.budgetScore
        binding.budgetPerformanceProgressBar.progress = summary.budgetPerformancePercentage
        
        // Update points this month
        binding.pointsThisMonthTextView.text = "${summary.pointsThisMonth} points earned"
        binding.pointsTrendTextView.text = summary.pointsTrend
        
        // Update overall performance
        binding.overallPerformanceTextView.text = summary.performanceMessage
        binding.overallGradeTextView.text = summary.overallGrade
        binding.gradeScaleTextView.text = "${summary.overallScore}/100"
        
        // Set colors based on performance
        val gradeColor = when (summary.overallGrade) {
            "A" -> android.graphics.Color.parseColor("#4CAF50") // Green
            "B" -> android.graphics.Color.parseColor("#8BC34A") // Light Green
            "C" -> android.graphics.Color.parseColor("#FFC107") // Amber
            "D" -> android.graphics.Color.parseColor("#FF9800") // Orange
            "F" -> android.graphics.Color.parseColor("#F44336") // Red
            else -> android.graphics.Color.GRAY
        }
        
        // Create circular background with the grade color
        val circleDrawable = android.graphics.drawable.GradientDrawable()
        circleDrawable.shape = android.graphics.drawable.GradientDrawable.OVAL
        circleDrawable.setColor(gradeColor)
        binding.overallGradeTextView.background = circleDrawable
        
        Log.d("RewardsFragment", "Updated performance summary: Grade ${summary.overallGrade}, Score ${summary.overallScore}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Important to avoid leaks with RecyclerViews in Fragments
        binding.badgesRecyclerView.adapter = null
        binding.leaderboardRecyclerView.adapter = null
        _binding = null
    }

    // Hide default ActionBar when this fragment is shown
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    // Show default ActionBar again if needed when leaving
    override fun onPause() {
        super.onPause()
        // Decide if you want to show it again when leaving, or keep it hidden
        // (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
} 