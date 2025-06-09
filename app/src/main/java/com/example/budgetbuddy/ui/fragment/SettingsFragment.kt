package com.example.budgetbuddy.ui.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.budgetbuddy.MainActivity
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentSettingsBinding
import com.example.budgetbuddy.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private val syncFrequencyOptions = arrayOf(
        "Every 30 minutes",
        "Every hour", 
        "Every 2 hours",
        "Every 6 hours",
        "Daily",
        "Manual only"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStatusBar()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupStatusBar() {
        // Force white status bar and hide MainActivity action bar
        activity?.window?.statusBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    private fun setupClickListeners() {
        // Back button
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Personal Information Section
        binding.editProfileRow.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
        }

        binding.changePasswordRow.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
        }

        // Notification Settings
        binding.budgetAlertsSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBudgetAlertsEnabled(isChecked)
        }

        binding.dailyRemindersSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDailyRemindersEnabled(isChecked)
        }

        // Cloud Sync Settings
        binding.autoSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoSyncEnabled(isChecked)
        }

        binding.syncFrequencyRow.setOnClickListener {
            showSyncFrequencyDialog()
        }

        // Data Management Section
        binding.exportDataRow.setOnClickListener {
            exportUserData()
        }

        binding.importDataRow.setOnClickListener {
            importUserData()
        }

        // General Settings
        binding.currencyRow.setOnClickListener {
            showCurrencyDialog()
        }

        // Account Section
        binding.signOutRow.setOnClickListener {
            showSignOutDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update user info
                    binding.userNameTextView.text = state.user?.name ?: "User"
                    binding.userEmailTextView.text = state.user?.email ?: "No email"
                    
                    // Update profile image
                    state.user?.profileImageUrl?.let { imageUrl ->
                        Glide.with(this@SettingsFragment)
                            .load(imageUrl)
                            .transform(CircleCrop())
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(binding.userProfileImageView)
                    } ?: run {
                        binding.userProfileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                    }

                    // Update notification settings
                    binding.budgetAlertsSwitch.isChecked = state.budgetAlertsEnabled
                    binding.dailyRemindersSwitch.isChecked = state.dailyRemindersEnabled

                    // Update sync settings
                    binding.autoSyncSwitch.isChecked = state.autoSyncEnabled
                    binding.syncFrequencyValueTextView.text = state.syncFrequency

                    // Update general settings
                    binding.currencyValueTextView.text = state.selectedCurrency

                    // Handle sign out result
                    if (state.signOutComplete) {
                        navigateToLogin()
                    }

                    // Handle errors
                    state.error?.let { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun showSyncFrequencyDialog() {
        val currentSelection = viewModel.uiState.value.syncFrequency
        val selectedIndex = syncFrequencyOptions.indexOf(currentSelection)

        AlertDialog.Builder(requireContext())
            .setTitle("Sync Frequency")
            .setSingleChoiceItems(syncFrequencyOptions, selectedIndex) { dialog, which ->
                viewModel.setSyncFrequency(syncFrequencyOptions[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out? Your data will be synced before signing out.")
            .setPositiveButton("Sign Out") { _, _ ->
                viewModel.signOut()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportUserData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Export Data")
            .setMessage("Export your budget data to a file? This will include all your expenses, budgets, and settings.")
            .setPositiveButton("Export") { _, _ ->
                Toast.makeText(context, "Data export feature will be implemented in a future update", Toast.LENGTH_LONG).show()
                // TODO: Implement data export functionality
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importUserData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Import Data")
            .setMessage("Import budget data from a file? This will overwrite your current data.")
            .setPositiveButton("Import") { _, _ ->
                Toast.makeText(context, "Data import feature will be implemented in a future update", Toast.LENGTH_LONG).show()
                // TODO: Implement data import functionality
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCurrencyDialog() {
        val currencies = arrayOf("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CNY", "INR")
        val currentCurrency = viewModel.uiState.value.selectedCurrency
        val selectedIndex = currencies.indexOf(currentCurrency)

        AlertDialog.Builder(requireContext())
            .setTitle("Select Currency")
            .setSingleChoiceItems(currencies, selectedIndex) { dialog, which ->
                val selectedCurrency = currencies[which]
                viewModel.setCurrency(selectedCurrency)
                Toast.makeText(context, "Currency changed to $selectedCurrency", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToLogin() {
        // Navigate back to login screen and clear back stack
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore action bar when leaving settings
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }
} 