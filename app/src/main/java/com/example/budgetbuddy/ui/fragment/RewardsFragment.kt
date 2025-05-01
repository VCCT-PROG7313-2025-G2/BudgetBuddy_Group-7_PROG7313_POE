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
import com.example.budgetbuddy.R
import com.example.budgetbuddy.adapter.BadgeAdapter
import com.example.budgetbuddy.adapter.LeaderboardAdapter
import com.example.budgetbuddy.databinding.FragmentRewardsBinding
import com.example.budgetbuddy.ui.viewmodel.RewardsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.view.isVisible
import androidx.appcompat.app.AppCompatActivity

@AndroidEntryPoint
class RewardsFragment : Fragment() {

    // --- Properties ---
    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RewardsViewModel by viewModels()
    private lateinit var badgeAdapter: BadgeAdapter
    private lateinit var leaderboardAdapter: LeaderboardAdapter

    // --- Lifecycle Methods ---
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

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        // Decide if you want to show it again when leaving, or keep it hidden
        // (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Important to avoid leaks with RecyclerViews in Fragments
        binding.badgesRecyclerView.adapter = null
        binding.leaderboardRecyclerView.adapter = null
        _binding = null
    }

    // --- UI Setup ---
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
    }

    // --- ViewModel Observation ---
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Handle Loading State
                    binding.loadingProgressBar.isVisible = state.isLoading
                    binding.mainContentGroup.isVisible = !state.isLoading

                    // Update UI only when not loading
                    if (!state.isLoading) {
                        // Update User Info
                        binding.userNameTextView.text = state.userName
                        // Combine level number and name
                        binding.userLevelTextView.text = "Level ${state.userLevel} ${state.userLevelName}"
                        // Load profile image - Use placeholder as userProfileImageUrl was removed from state
                        Glide.with(this@RewardsFragment)
                             .load(R.drawable.ic_profile_placeholder) // Use placeholder
                             .circleCrop()
                             .placeholder(R.drawable.ic_profile_placeholder)
                             .into(binding.userProfileImageView)

                        // Update Next Reward section based on points/level
                        // Use nextLevelPoints from state if needed for text, otherwise remove or adjust
                        binding.nextRewardNameTextView.text = "Next Level: ${state.nextLevelPoints} pts"
                        binding.nextRewardProgressBar.max = state.pointsNeededForLevel
                        binding.nextRewardProgressBar.progress = state.pointsInLevel
                        // Calculate percentage text based on actual points
                        val progressPercent = if (state.pointsNeededForLevel > 0) {
                            ((state.pointsInLevel.toDouble() / state.pointsNeededForLevel) * 100).toInt()
                        } else {
                            100 // Handle edge case where level requires 0 points (shouldn't happen with coerceAtLeast(1))
                        }
                        binding.nextRewardPercentageTextView.text = "$progressPercent%"

                        // *** Add Logging Here ***
                        Log.d("RewardsFragment", "Updating badge adapter. Badge count: ${state.achievements.size}")
                        state.achievements.forEach { badge ->
                             Log.d("RewardsFragment", "  - Badge: ${badge.name}, ID: ${badge.id}, IconRes: ${badge.iconResId}")
                        }
                        // ***********************

                        // Update Badges RecyclerView using state.achievements
                        badgeAdapter.updateData(state.achievements) // Use the correct field name

                        // *** Add Logging Here Too ***
                        Log.d("RewardsFragment", "Updating leaderboard adapter. Entry count: ${state.leaderboardEntries.size}")
                        // **************************

                        // Update Leaderboard RecyclerView using state.leaderboardEntries
                        leaderboardAdapter.updateData(state.leaderboardEntries) // Use the correct field name
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

    // --- Helper Functions ---
    private fun shareRewards() {
        val state = viewModel.uiState.value // Get the current state
        val points = state.currentPoints   // Use correct field name
        val level = state.userLevelName // Use correct field name

        // Construct the share message
        val shareText = "Check out my progress on BudgetBuddy! I\'m level \"$level\" with $points points. Join me in managing finances! #BudgetBuddyApp"
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
} 