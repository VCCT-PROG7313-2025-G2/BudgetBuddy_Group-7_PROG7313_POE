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
import com.example.budgetbuddy.databinding.FragmentChangePasswordBinding
import com.example.budgetbuddy.ui.viewmodel.ChangePasswordViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChangePasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
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
        
        // Set action bar color to black before hiding to prevent purple flash
        (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            // Set the action bar background to black before hiding
            actionBar.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.BLACK))
            actionBar.hide()
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.changePasswordButton.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val currentPassword = binding.currentPasswordEditText.text.toString().trim()
        val newPassword = binding.newPasswordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        // Clear previous errors
        binding.currentPasswordLayout.error = null
        binding.newPasswordLayout.error = null
        binding.confirmPasswordLayout.error = null

        // Validate inputs
        if (currentPassword.isEmpty()) {
            binding.currentPasswordLayout.error = "Current password is required"
            return
        }

        if (newPassword.isEmpty()) {
            binding.newPasswordLayout.error = "New password is required"
            return
        }

        if (newPassword.length < 8) {
            binding.newPasswordLayout.error = "Password must be at least 8 characters"
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordLayout.error = "Please confirm your new password"
            return
        }

        if (newPassword != confirmPassword) {
            binding.confirmPasswordLayout.error = "Passwords do not match"
            return
        }

        if (currentPassword == newPassword) {
            binding.newPasswordLayout.error = "New password must be different from current password"
            return
        }

        // Call ViewModel to change password
        viewModel.changePassword(currentPassword, newPassword)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update loading state
                    binding.progressBar.isVisible = state.isLoading
                    binding.changePasswordButton.isEnabled = !state.isLoading

                    // Handle password change success
                    if (state.passwordChangeComplete) {
                        Toast.makeText(context, "Password changed successfully", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    }

                    // Handle errors
                    state.error?.let { error ->
                        when {
                            error.contains("current password", ignoreCase = true) -> {
                                binding.currentPasswordLayout.error = error
                            }
                            error.contains("new password", ignoreCase = true) -> {
                                binding.newPasswordLayout.error = error
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
        
        // Restore action bar when leaving change password with correct black color
        (activity as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            // Set the action bar background to black before showing
            actionBar.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.BLACK))
            actionBar.show()
        }
        
        _binding = null
    }
} 