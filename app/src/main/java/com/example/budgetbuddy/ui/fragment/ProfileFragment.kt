package com.example.budgetbuddy.ui.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Removed profileImageUri logic for now as edit button is separate
    // private var profileImageUri: Uri? = null
    // private val pickImageLauncher = ...

    // Get reference to the ViewModel
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Remove direct data loading, rely on observation
        // loadProfileData()
        setupClickListeners()
        observeViewModel() // Start observing the ViewModel
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.profileNameTextView.text = state.userName
                    binding.profileEmailTextView.text = state.userEmail
                    binding.budgetAmountTextView.text = state.budgetLimitText
                    binding.budgetRemainingTextView.text = state.budgetRemainingText
                    binding.budgetProgressBar.progress = state.budgetProgress

                    // Load profile image (using Glide as an example)
                    Glide.with(this@ProfileFragment)
                        .load(state.profileImageUrl ?: R.drawable.ic_profile_placeholder) // Use URL or placeholder
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(binding.profileImageView)

                    // Handle loading state (optional: show a spinner)
                    // binding.loadingSpinner.isVisible = state.isLoading

                    // Handle error state
                    state.error?.let {
                        Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
                        // Reset error in ViewModel or handle appropriately
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Removed changePictureButton listener

        // Handle Edit Profile Button Click
        binding.editProfileButton.setOnClickListener {
            // TODO: Navigate to an EditProfileFragment (needs to be created)
            Toast.makeText(context, "Edit Profile Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        // Handle Expense History Row Click
        binding.expenseHistoryRow.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_expenseHistoryFragment)
        }

        // Handle Settings Row Click
        binding.settingsRow.setOnClickListener {
            // Navigate to Settings screen using defined action
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        // Removed budgetSetupLinkTextView listener (if not needed here)

        // Removed saveProfileButton listener

        // Removed saveProfileChanges() method as edit/save is likely moved
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

    // Show default ActionBar again when leaving (optional)
    override fun onPause() {
        super.onPause()
        // Keep hidden if navigating within profile/settings sections
        // (activity as? AppCompatActivity)?.supportActionBar?.show()
    }
} 