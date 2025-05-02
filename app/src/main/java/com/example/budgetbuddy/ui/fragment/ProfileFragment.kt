package com.example.budgetbuddy.ui.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentProfileBinding
import com.example.budgetbuddy.ui.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// Marks this Fragment for Hilt injection.
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    // --- View Binding --- 
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Image picking logic is likely moved to EditProfileFragment.
    // private var profileImageUri: Uri? = null
    // private val pickImageLauncher = ...

    // --- ViewModel --- 
    private val viewModel: ProfileViewModel by viewModels()

    // --- Lifecycle Methods --- 
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout.
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up button click actions.
        setupClickListeners()
        // Start observing data updates from the ViewModel.
        observeViewModel()
    }

    // --- ViewModel Observation --- 
    // Observes the UI state from the ProfileViewModel and updates the screen.
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect the latest UI state.
                viewModel.uiState.collect { state ->
                    // Update name, email, and budget information text views.
                    binding.profileNameTextView.text = state.userName
                    binding.profileEmailTextView.text = state.userEmail
                    binding.budgetAmountTextView.text = state.budgetLimitText
                    binding.budgetRemainingTextView.text = state.budgetRemainingText
                    binding.budgetProgressBar.progress = state.budgetProgress

                    // Load the profile image using Glide.
                    // Uses the URL from the state, or a placeholder if no image is set.
                    Glide.with(this@ProfileFragment)
                        .load(state.profileImageUrl ?: R.drawable.ic_profile_placeholder)
                        .circleCrop() // Make the image circular.
                        .placeholder(R.drawable.ic_profile_placeholder) // Show placeholder while loading.
                        .into(binding.profileImageView)

                    // Optional: Show/hide a loading indicator.
                    // binding.loadingSpinner.isVisible = state.isLoading

                    // Show error messages if any.
                    state.error?.let {
                        Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
                        // TODO: Consider calling a ViewModel function to clear the error.
                    }
                }
            }
        }
    }

    // --- Click Listeners --- 
    // Sets up navigation actions for buttons and rows.
    private fun setupClickListeners() {
        // Edit Profile button: Navigate to the Edit Profile screen.
        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        // Expense History row: Navigate to the Expense History screen.
        binding.expenseHistoryRow.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_expenseHistoryFragment)
        }

        // Settings row: Navigate to the Settings screen.
        binding.settingsRow.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        // Other listeners (change picture, save profile) were likely moved to EditProfileFragment.
    }

    // --- Lifecycle Cleanup --- 
    // Cleans up View Binding when the fragment's view is destroyed.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Hide the main Activity's toolbar when this fragment is displayed.
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    // Optional: Restore the toolbar when leaving (currently commented out).
    override fun onPause() {
        super.onPause()
        // (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
} 