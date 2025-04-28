package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Placeholder for actions
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Import for Hilt ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentLoginSignupBinding
import com.example.budgetbuddy.ui.viewmodel.AuthUiState
import com.example.budgetbuddy.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginSignupFragment : Fragment() {

    private var _binding: FragmentLoginSignupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
         binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(context, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.signUpButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginSignupFragment_to_accountCreationFragment)
        }

        binding.forgotPasswordButton.setOnClickListener {
            Toast.makeText(context, "Forgot Password clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        // TODO: Add logic for Biometric Login button
        binding.biometricLoginButton.setOnClickListener {
             Toast.makeText(context, "Biometric Login clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Handle UI state changes
                    binding.loginButton.isEnabled = state !is AuthUiState.Loading
                    // TODO: Add progress indicator visibility
                    // binding.loadingIndicator.isVisible = state is AuthUiState.Loading

                    when (state) {
                        is AuthUiState.Success -> {
                            Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to Home screen after successful login
                            findNavController().navigate(R.id.action_loginSignupFragment_to_homeFragment)
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
} 