package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.saveButton.setOnClickListener {
            // TODO: Call ViewModel to save profile changes
            Toast.makeText(context, "Save Clicked (Not Implemented)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update fields only if not loading and text differs (prevents loop)
                    if (!state.isLoading && binding.nameEditText.text?.toString() != state.currentName) {
                        binding.nameEditText.setText(state.currentName)
                    }
                    if (!state.isLoading && binding.emailEditText.text?.toString() != state.currentEmail) {
                        binding.emailEditText.setText(state.currentEmail)
                    }

                    binding.saveButton.isEnabled = !state.isLoading
                    // Optionally show/hide a progress indicator tied to state.isLoading

                    if (state.isSuccess) {
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack() // Go back to previous screen (Settings)
                        // Optionally reset success state in ViewModel if needed to prevent re-triggering
                        // viewModel.resetSuccessState() 
                    }

                    state.error?.let {
                        Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
                        // Optionally reset error state in ViewModel
                        // viewModel.clearError()
                    }
                }
            }
        }
    }

    // Hide default ActionBar when this fragment is shown
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 