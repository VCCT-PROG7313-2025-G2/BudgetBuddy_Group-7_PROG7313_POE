package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Placeholder for actions
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentLoginSignupBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginSignupFragment : Fragment() {

    private var _binding: FragmentLoginSignupBinding? = null
    private val binding get() = _binding!!

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

        binding.loginButton.setOnClickListener {
            // TODO: Implement login logic (validation, authentication)
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            // Placeholder action: Navigate to home (needs nav graph update)
             findNavController().navigate(R.id.action_loginSignupFragment_to_homeFragment) // Needs action ID
            Toast.makeText(context, "Login clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        binding.signUpButton.setOnClickListener {
            // Navigate to Account Creation screen
             findNavController().navigate(R.id.action_loginSignupFragment_to_accountCreationFragment) // Needs action ID
        }

        binding.forgotPasswordButton.setOnClickListener {
            // TODO: Implement forgot password flow
            Toast.makeText(context, "Forgot Password clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        // TODO: Add logic for Biometric Login if needed
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 