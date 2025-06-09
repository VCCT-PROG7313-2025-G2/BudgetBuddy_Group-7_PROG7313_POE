package com.example.budgetbuddy.ui.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentEditProfileBinding
import com.example.budgetbuddy.ui.viewmodel.EditProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    // Activity result launchers for image selection
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            loadImageIntoView(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && selectedImageUri != null) {
            loadImageIntoView(selectedImageUri!!)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(context, "Camera permission required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStatusBar()
        setupClickListeners()
        observeViewModel()
        
        // Load user data
        viewModel.loadUserProfile()
    }

    private fun setupStatusBar() {
        // Force white status bar and hide MainActivity action bar
        activity?.window?.statusBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveProfile()
        }

        binding.editProfileImageFab.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Remove Photo")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Change Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpen()
                    1 -> openGallery()
                    2 -> removeProfilePhoto()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        try {
            // Create a temporary file URI for the camera result
            selectedImageUri = viewModel.createTempImageUri(requireContext())
            selectedImageUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun removeProfilePhoto() {
        selectedImageUri = null
        binding.profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
        Toast.makeText(context, "Profile photo will be removed", Toast.LENGTH_SHORT).show()
    }

    private fun loadImageIntoView(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .transform(CircleCrop())
            .placeholder(R.drawable.ic_profile_placeholder)
            .error(R.drawable.ic_profile_placeholder)
            .into(binding.profileImageView)
    }

    private fun saveProfile() {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()

        // Clear previous errors
        binding.nameInputLayout.error = null
        binding.emailInputLayout.error = null

        // Validate inputs
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            return
        }

        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Please enter a valid email address"
            return
        }

        // Call ViewModel to save profile with image
        viewModel.updateProfile(name, email, selectedImageUri)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update loading state
                    binding.saveButton.isEnabled = !state.isLoading
                    
                    // Load user data into fields
                    state.user?.let { user ->
                        if (binding.nameEditText.text.toString() != user.name) {
                            binding.nameEditText.setText(user.name)
                        }
                        if (binding.emailEditText.text.toString() != user.email) {
                            binding.emailEditText.setText(user.email)
                        }
                        
                        // Load profile image
                        user.profileImageUrl?.let { imageUrl ->
                            if (selectedImageUri == null) { // Only load if user hasn't selected a new image
                                Glide.with(this@EditProfileFragment)
                                    .load(imageUrl)
                                    .transform(CircleCrop())
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .into(binding.profileImageView)
                            }
                        }
                    }

                    // Handle profile update success
                    if (state.profileUpdateComplete) {
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    }

                    // Handle errors
                    state.error?.let { error ->
                        when {
                            error.contains("email", ignoreCase = true) -> {
                                binding.emailInputLayout.error = error
                            }
                            error.contains("name", ignoreCase = true) -> {
                                binding.nameInputLayout.error = error
                            }
                            else -> {
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        }
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore action bar when leaving edit profile
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }
} 