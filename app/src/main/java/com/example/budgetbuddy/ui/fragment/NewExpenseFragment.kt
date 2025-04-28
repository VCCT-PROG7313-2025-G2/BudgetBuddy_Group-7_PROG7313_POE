package com.example.budgetbuddy.ui.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentNewExpenseBinding
import com.example.budgetbuddy.ui.viewmodel.NewExpenseUiState
import com.example.budgetbuddy.ui.viewmodel.NewExpenseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class NewExpenseFragment : Fragment() {

    private var _binding: FragmentNewExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewExpenseViewModel by viewModels()
    private var selectedDate: Date = Date() // Store selected date
    // TODO: Add logic for handling receipt attachment URI/path
    private var receiptPath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategorySpinner()
        setupDatePicker()
        setupClickListeners()
        observeViewModel()
        updateDateDisplay() // Show initial date
    }

    private fun setupCategorySpinner() {
        // Use the same restricted list of categories as the home screen
        val categories = listOf("Food & Dining", "Transport", "Shopping", "Utilities", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.categoryAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        binding.dateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate // Start picker at currently selected date

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    updateDateDisplay()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        binding.dateEditText.setText(dateFormat.format(selectedDate))
    }

    private fun setupClickListeners() {
        binding.saveExpenseButton.setOnClickListener {
            saveExpense()
        }
        binding.attachReceiptButton.setOnClickListener {
            // TODO: Implement image picking logic (Activity Result API)
            Toast.makeText(context, "Attach receipt not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveExpense() {
        val amountText = binding.amountEditText.text.toString()
        val amount = try {
            BigDecimal(amountText)
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val category = binding.categoryAutoCompleteTextView.text.toString()
        val notes = binding.notesEditText.text.toString()

        viewModel.saveExpense(amount, category, selectedDate, notes, receiptPath)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.saveExpenseButton.isEnabled = state !is NewExpenseUiState.Loading
                    // TODO: Show loading indicator

                    when (state) {
                        is NewExpenseUiState.Success -> {
                            Toast.makeText(context, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp() // Go back
                            viewModel.resetState()
                        }
                        is NewExpenseUiState.Error -> {
                            Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        else -> Unit // Idle or Loading
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 