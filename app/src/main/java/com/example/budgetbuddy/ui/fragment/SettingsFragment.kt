package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R // Ensure R is imported
import com.example.budgetbuddy.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        // TODO: Load actual settings from SharedPreferences or ViewModel/Repository
        binding.budgetAlertsSwitch.isChecked = true // Placeholder
        binding.dailyRemindersSwitch.isChecked = true // Placeholder - Renamed from goalRemindersSwitch
        // Update currency text if dynamic
        binding.syncFrequencyValueTextView.text = "Every 2 hours" // Placeholder
    }

    private fun setupListeners() {
        binding.editProfileRow.setOnClickListener { /* TODO */ Toast.makeText(context, "Edit Profile", Toast.LENGTH_SHORT).show() }
        binding.changePasswordRow.setOnClickListener { /* TODO */ Toast.makeText(context, "Change Password", Toast.LENGTH_SHORT).show() }

        binding.budgetAlertsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save budget alerts setting
            println("Budget Alerts: $isChecked")
        }

        binding.dailyRemindersSwitch.setOnCheckedChangeListener { _, isChecked -> // Renamed from goalRemindersSwitch
            // TODO: Save daily reminders setting
            println("Daily Reminders: $isChecked")
        }

        binding.autoSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save auto-sync setting
            println("Auto Sync: $isChecked")
        }

        binding.syncFrequencyRow.setOnClickListener {
            // TODO: Implement sync frequency selection
            Toast.makeText(context, "Sync Frequency", Toast.LENGTH_SHORT).show()
        }

        binding.signOutRow.setOnClickListener {
            // TODO: Implement logout logic (clear session, navigate to login)
            Toast.makeText(context, "Sign Out", Toast.LENGTH_SHORT).show()
        }

        // Remove listeners for views that were removed from the layout
        // binding.currencyTextView.setOnClickListener { ... }
        // binding.exportDataTextView.setOnClickListener { ... }
        // binding.importDataTextView.setOnClickListener { ... }
        // binding.logoutButton.setOnClickListener { ... }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 