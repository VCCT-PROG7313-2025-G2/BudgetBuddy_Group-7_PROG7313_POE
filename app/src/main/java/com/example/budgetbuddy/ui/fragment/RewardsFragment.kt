package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetbuddy.R
import com.example.budgetbuddy.adapter.AchievementAdapter
import com.example.budgetbuddy.databinding.FragmentRewardsBinding
import com.example.budgetbuddy.model.Achievement
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!

    private lateinit var achievementAdapter: AchievementAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadRewardsData() // Load placeholder data
    }

    private fun setupRecyclerView() {
        achievementAdapter = AchievementAdapter { achievement ->
            // Handle achievement click (e.g., show details)
            Toast.makeText(context, "Clicked on: ${achievement.name}", Toast.LENGTH_SHORT).show()
        }
        binding.achievementsRecyclerView.apply {
            adapter = achievementAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun loadRewardsData() {
        // TODO: Load actual user points, level, and achievements from ViewModel/Repository
        val userPoints = 1250
        val currentLevelPoints = 600 // Points within the current level
        val pointsForNextLevel = 1000 // Total points needed for the next level
        val pointsToGo = pointsForNextLevel - currentLevelPoints
        val levelName = "Level 5: Budget Master"

        val format: NumberFormat = NumberFormat.getInstance(Locale.getDefault())
        binding.pointsValueTextView.text = format.format(userPoints)
        binding.levelTextView.text = levelName
        binding.pointsToNextLevelTextView.text = getString(R.string.points_to_next_level_format, pointsToGo)
        binding.levelProgressBar.max = pointsForNextLevel
        binding.levelProgressBar.progress = currentLevelPoints

        achievementAdapter.submitList(getPlaceholderAchievements())
    }

    // --- Placeholder Data Generation --- 
    private fun getPlaceholderAchievements(): List<Achievement> {
        return listOf(
            Achievement("budget1", "First Budget Set", "You successfully set your first monthly budget!", R.drawable.ic_achievement_unlocked, true),
            Achievement("expense1", "First Expense Added", "Tracked your first expense.", R.drawable.ic_achievement_unlocked, true),
            Achievement("streak7", "7-Day Tracking Streak", "Tracked expenses for 7 days in a row.", R.drawable.ic_achievement_locked, false),
            Achievement("saving1", "Savings Goal Started", "Created your first savings goal.", R.drawable.ic_achievement_locked, false),
            Achievement("report1", "Viewed First Report", "Checked out your spending insights.", R.drawable.ic_achievement_unlocked, true)
        )
    }
    // --- End Placeholder Data ---

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 