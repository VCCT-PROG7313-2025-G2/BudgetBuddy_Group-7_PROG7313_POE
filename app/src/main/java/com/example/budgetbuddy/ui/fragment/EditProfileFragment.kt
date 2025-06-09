package com.example.budgetbuddy.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.databinding.FragmentEditProfileBinding
import com.example.budgetbuddy.ui.viewmodel.EditProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels()

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
        // Force white status bar (action bar hiding handled by MainActivity)
        activity?.window?.statusBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveProfile()
        }
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
        viewModel.updateProfile(name, email)
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
        
        // Don't manually show/hide action bar - let MainActivity handle it
        _binding = null
    }
} 