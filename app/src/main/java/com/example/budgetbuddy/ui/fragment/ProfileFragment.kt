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

    private var profileImageUri: Uri? = null

    // Activity Result Launcher for picking profile image
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            profileImageUri = result.data?.data
            binding.profileImageView.setImageURI(profileImageUri)
            // TODO: Upload image if necessary
        } 
    }

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
        binding.nameEditText.setText("Alex Johnson")
        binding.emailEditText.setText("alex.j@example.com")
        // TODO: Load profile picture (e.g., using Glide or Coil)
        binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
    }

    private fun setupClickListeners() {
        binding.changePictureButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.settingsLinkTextView.setOnClickListener {
            // Navigate to Settings screen using defined action
             findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
             // Toast.makeText(context, "Settings Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        binding.budgetSetupLinkTextView.setOnClickListener {
            // Navigate to Budget Setup screen using defined action
             findNavController().navigate(R.id.action_profileFragment_to_budgetSetupFragment)
        }

        binding.saveProfileButton.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun saveProfileChanges() {
        val newName = binding.nameEditText.text.toString()

        if (newName.isBlank()) {
            Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Implement actual saving logic using ViewModel/Repository
        // Save newName and potentially the new profileImageUri
        println("Saving Profile:")
        println(" Name: $newName")
        println(" Profile Image URI: $profileImageUri")

        Toast.makeText(context, "Profile Saved (Not implemented)", Toast.LENGTH_SHORT).show()
        // Optionally navigate back or just show confirmation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 