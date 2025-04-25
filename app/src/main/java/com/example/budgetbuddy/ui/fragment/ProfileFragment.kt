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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Removed profileImageUri logic for now as edit button is separate
    // private var profileImageUri: Uri? = null
    // private val pickImageLauncher = ...

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfileData()
        setupClickListeners()
    }

    private fun loadProfileData() {
        // TODO: Load actual user data from ViewModel/Repository
        binding.profileNameTextView.text = "Alex Johnson"
        binding.profileEmailTextView.text = "alex.j@example.com"
        // TODO: Load profile picture (e.g., using Glide or Coil)
        binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)

        // TODO: Load actual budget overview data
        binding.budgetAmountTextView.text = "$3,500 Limit" 
        binding.budgetProgressBar.progress = 65
        binding.budgetRemainingTextView.text = "$1,225 remaining"
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
            // TODO: Navigate to an ExpenseHistoryFragment (needs to be created or use ExpensesFragment)
            // Example: findNavController().navigate(R.id.action_profileFragment_to_expensesFragment)
            Toast.makeText(context, "Expense History Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        // Handle Settings Row Click
        binding.settingsRow.setOnClickListener {
            // Navigate to Settings screen using defined action
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        // Removed budgetSetupLinkTextView listener (if not needed here)

        // Removed saveProfileButton listener
    }

    // Removed saveProfileChanges() method as edit/save is likely moved
    // private fun saveProfileChanges() { ... }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 