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

// Marks this Fragment for Hilt dependency injection.
@AndroidEntryPoint
class LoginSignupFragment : Fragment() {

    // View Binding to access layout elements.
    private var _binding: FragmentLoginSignupBinding? = null
    private val binding get() = _binding!!

    // Get the ViewModel associated with authentication.
    private val viewModel: AuthViewModel by viewModels()

    // Called when the fragment's view is being created.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment.
        _binding = FragmentLoginSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Called after the view has been created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set up what happens when buttons are clicked.
        setupClickListeners()
        // Start observing data changes from the ViewModel.
        observeViewModel()
    }

    // Configures the actions for button clicks.
    private fun setupClickListeners() {
        // When the Login button is clicked...
         binding.loginButton.setOnClickListener {
             // Get email and password from the input fields.
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            // Basic check: make sure fields are not empty.
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Tell the ViewModel to attempt login.
                viewModel.login(email, password)
            } else {
                // Show a message if fields are empty.
                Toast.makeText(context, "Email and password cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        // When the Sign Up button is clicked...
        binding.signUpButton.setOnClickListener {
            // Navigate to the Account Creation screen.
            findNavController().navigate(R.id.action_loginSignupFragment_to_accountCreationFragment)
        }

        // When Forgot Password is clicked (currently shows a placeholder message).
        binding.forgotPasswordButton.setOnClickListener {
            Toast.makeText(context, "Forgot Password clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        // TODO: Add logic for Biometric Login button
        // When Biometric Login is clicked (currently shows a placeholder message).
        binding.biometricLoginButton.setOnClickListener {
             Toast.makeText(context, "Biometric Login clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }
    }

    // Observes the UI state from AuthViewModel.
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect the latest state updates.
                viewModel.uiState.collect { state ->
                    // Enable/disable the login button based on whether loading is in progress.
                    binding.loginButton.isEnabled = state !is AuthUiState.Loading
                    // TODO: Add a visual loading indicator (spinner).
                    // binding.loadingIndicator.isVisible = state is AuthUiState.Loading

                    // Handle the different states (Success, Error, Loading, Idle).
                    when (state) {
                        is AuthUiState.Success -> {
                            // Login was successful!
                            Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                            // Navigate to the main Home screen.
                            findNavController().navigate(R.id.action_loginSignupFragment_to_homeFragment)
                            // Reset the ViewModel state to avoid re-navigating if the screen is revisited.
                            viewModel.resetState()
                        }
                        is AuthUiState.Error -> {
                            // Show an error message if login failed.
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> Unit // Do nothing for Idle or Loading states here.
                    }
                }
            }
        }
    }

    // Hide the main Activity's toolbar when this fragment is shown.
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    // Show the main Activity's toolbar again when leaving this fragment.
    override fun onPause() {
        super.onPause()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        // Optionally reset state if user navigates back (could be useful).
        // viewModel.resetState()
    }

    // Clean up the binding when the view is destroyed.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 