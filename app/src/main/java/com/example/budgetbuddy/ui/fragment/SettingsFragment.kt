package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity // Import for ActionBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Import viewModels delegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R // Ensure R is imported
import com.example.budgetbuddy.databinding.FragmentSettingsBinding
import com.example.budgetbuddy.ui.viewmodel.SettingsEvent // Import event
import com.example.budgetbuddy.ui.viewmodel.SettingsViewModel // Import ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch // Import launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Get reference to the ViewModel
    private val viewModel: SettingsViewModel by viewModels()

    // Define sync frequency options
    private val syncFrequencyOptions by lazy {
        arrayOf("Manual", "Every hour", "Every 2 hours", "Daily") // Can be moved to resources
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        // Setup Click Listeners
        setupClickListeners()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings()
        setupListeners()
        observeViewModelEvents() // Observe navigation/other events
    }

    private fun loadSettings() {
        // TODO: Load actual settings from SharedPreferences or ViewModel/Repository
        binding.budgetAlertsSwitch.isChecked = true // Placeholder
        binding.dailyRemindersSwitch.isChecked = true // Placeholder - Renamed from goalRemindersSwitch
        // TODO: Load saved sync frequency preference here
        binding.syncFrequencyValueTextView.text = syncFrequencyOptions[2] // Default placeholder to "Every 2 hours"
    }

    private fun setupListeners() {
        binding.editProfileRow.setOnClickListener {
            // Navigate to Edit Profile using the new action
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
        }
        binding.changePasswordRow.setOnClickListener {
             // Navigate to Change Password using the new action
            findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
        }

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
            showSyncFrequencyDialog()
        }

        binding.signOutRow.setOnClickListener {
            viewModel.onSignOutClicked() // Call ViewModel function
        }

        // Remove listeners for views that were removed from the layout
        // binding.currencyTextView.setOnClickListener { ... }
        // binding.exportDataTextView.setOnClickListener { ... }
        // binding.importDataTextView.setOnClickListener { ... }
        // binding.logoutButton.setOnClickListener { ... }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // TODO: Add listeners for other rows (Edit Profile, Change Password, Sign Out, etc.)
        binding.editProfileRow.setOnClickListener {
            // Example: navigate to edit profile screen
             Toast.makeText(context, "Edit Profile Clicked", Toast.LENGTH_SHORT).show()
        }
        binding.changePasswordRow.setOnClickListener {
             Toast.makeText(context, "Change Password Clicked", Toast.LENGTH_SHORT).show()
        }
        binding.signOutRow.setOnClickListener {
            // TODO: Implement sign out logic (e.g., call ViewModel)
             Toast.makeText(context, "Sign Out Clicked", Toast.LENGTH_SHORT).show()
        }
        // Add listeners for switches if needed
    }

    // Observe one-time events from the ViewModel
    private fun observeViewModelEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collect { event ->
                    when (event) {
                        is SettingsEvent.NavigateToLogin -> {
                            // Navigate to the login/auth graph start destination
                            // Replace R.id.auth_graph with your actual nav graph ID if different
                            // Use popUpTo to clear the back stack up to the main graph
                            findNavController().navigate(R.id.auth_graph, null, 
                                androidx.navigation.NavOptions.Builder()
                                    .setPopUpTo(R.id.main_graph, true) // Pop back stack to main graph start, inclusive
                                    .build()
                            )
                        }
                        // Handle other events like errors if added later
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
        // Keep hidden
        // (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    private fun showSyncFrequencyDialog() {
        val currentSelection = binding.syncFrequencyValueTextView.text.toString()
        var checkedItemIndex = syncFrequencyOptions.indexOf(currentSelection)
        if (checkedItemIndex == -1) {
            checkedItemIndex = 2 // Default to "Every 2 hours" if current text not found
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Sync Frequency")
            .setSingleChoiceItems(syncFrequencyOptions, checkedItemIndex) { dialog, which ->
                // Store the selection index temporarily when an item is clicked
                checkedItemIndex = which
            }
            .setPositiveButton("OK") { dialog, _ ->
                // Update the TextView with the selected option
                val selectedFrequency = syncFrequencyOptions[checkedItemIndex]
                binding.syncFrequencyValueTextView.text = selectedFrequency
                // TODO: Save the selectedFrequency preference (e.g., call ViewModel)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
} 