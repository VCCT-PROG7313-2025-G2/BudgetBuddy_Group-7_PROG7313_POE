package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Placeholder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Import for Hilt ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentAccountCreationBinding
import com.example.budgetbuddy.ui.viewmodel.AuthUiState
import com.example.budgetbuddy.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountCreationFragment : Fragment() {

    // --- Properties --- 
    private var _binding: FragmentAccountCreationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    // --- Lifecycle Methods --- 
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountCreationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        // Optionally reset state if user navigates back
        // viewModel.resetState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- UI Setup --- 
    private fun setupClickListeners() {
         binding.signUpButton.setOnClickListener {
            val fullName = binding.fullNameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            val termsAccepted = binding.termsCheckBox.isChecked

            // Basic Validation
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                 Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
             if (!termsAccepted) {
                Toast.makeText(context, "Please accept the terms and policy.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // TODO: Add more robust email/password validation

            viewModel.signup(fullName, email, password)
        }

        binding.backToLoginButton.setOnClickListener {
            findNavController().navigateUp() // Go back to previous screen (Login)
        }
    }

    // --- ViewModel Observation --- 
     private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Handle UI state changes
                    binding.signUpButton.isEnabled = state !is AuthUiState.Loading
                    // TODO: Add progress indicator visibility
                    // binding.loadingIndicator.isVisible = state is AuthUiState.Loading

                    when (state) {
                        is AuthUiState.Success -> {
                            Toast.makeText(context, "Signup Successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to Home screen after successful signup
                            findNavController().navigate(R.id.action_accountCreationFragment_to_homeFragment)
                            viewModel.resetState() // Reset state after navigation
                        }
                        is AuthUiState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> Unit // Idle or Loading
                    }
                }
            }
        }
    }
} 