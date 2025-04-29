package com.example.budgetbuddy.ui.fragment

import android.content.Intent
import android.os.Bundle
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

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RewardsViewModel by viewModels()
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
    }

    private fun shareRewards() {
        // Create the text content to share
        // TODO: Customize this text with actual data from ViewModel if needed
        val shareText = "Check out my progress on BudgetBuddy! Level: ${viewModel.uiState.value.userLevel}, Next Reward: ${viewModel.uiState.value.nextRewardName}"

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Share your progress via")
        try {
            startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share options.", Toast.LENGTH_SHORT).show()
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
                        // Update User Info
                        binding.userNameTextView.text = state.userName
                        binding.userLevelTextView.text = state.userLevel
                        // Load profile image (e.g., using Glide) - Optional
                        Glide.with(this@RewardsFragment)
                             .load(state.userProfileImageUrl ?: R.drawable.ic_profile_placeholder)
                             .circleCrop() // Make it circular
                             .placeholder(R.drawable.ic_profile_placeholder)
                             .into(binding.userProfileImageView)

                        // Update Next Reward
                        binding.nextRewardNameTextView.text = state.nextRewardName
                        binding.nextRewardProgressBar.progress = state.nextRewardProgress
                        binding.nextRewardPercentageTextView.text = "${state.nextRewardProgress}%"

                        // Update Badges RecyclerView
                        badgeAdapter.updateData(state.badges) // Assuming adapter has updateData

                        // Update Leaderboard RecyclerView
                        leaderboardAdapter.updateData(state.leaderboard) // Assuming adapter has updateData
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