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
        binding.goalRemindersSwitch.isChecked = true // Placeholder
        // Update currency text if dynamic
    }

    private fun setupListeners() {
        binding.budgetAlertsSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save budget alerts setting
            println("Budget Alerts: $isChecked")
        }

        binding.goalRemindersSwitch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save goal reminders setting
            println("Goal Reminders: $isChecked")
        }

        binding.currencyTextView.setOnClickListener {
            // TODO: Implement currency selection (e.g., show a dialog)
            Toast.makeText(context, "Currency selection not implemented", Toast.LENGTH_SHORT).show()
        }

        binding.exportDataTextView.setOnClickListener {
            // TODO: Implement export data functionality
            Toast.makeText(context, "Export data not implemented", Toast.LENGTH_SHORT).show()
        }

        binding.importDataTextView.setOnClickListener {
            // TODO: Implement import data functionality
            Toast.makeText(context, "Import data not implemented", Toast.LENGTH_SHORT).show()
        }

        binding.logoutButton.setOnClickListener {
            // TODO: Implement logout logic (clear session, navigate to login)
            Toast.makeText(context, "Logout Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
            // Example navigation back to the start (assuming nav_graph is the root)
            // findNavController().navigate(R.id.action_global_startupFragment) // Needs global action
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 