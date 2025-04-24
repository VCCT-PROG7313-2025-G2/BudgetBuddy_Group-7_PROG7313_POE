package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Placeholder
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentAccountCreationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountCreationFragment : Fragment() {

    private var _binding: FragmentAccountCreationBinding? = null
    private val binding get() = _binding!!

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

        binding.signUpButton.setOnClickListener {
            // TODO: Implement account creation logic (validation, saving data, authentication)
            val fullName = binding.fullNameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            val termsAccepted = binding.termsCheckBox.isChecked

            if (!termsAccepted) {
                Toast.makeText(context, "Please accept the terms and policy.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                 Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Placeholder action: Navigate to home (needs nav graph update)
            // Usually, after signup, you might log the user in automatically and navigate home
             findNavController().navigate(R.id.action_accountCreationFragment_to_homeFragment) // Needs action defined
            Toast.makeText(context, "Sign Up successful (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        binding.backToLoginButton.setOnClickListener {
            // Navigate back to Login/Sign Up screen using the popUpTo action defined in nav_graph
            findNavController().navigate(R.id.action_accountCreationFragment_to_loginSignupFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 