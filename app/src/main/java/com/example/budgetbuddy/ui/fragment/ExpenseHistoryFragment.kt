package com.example.budgetbuddy.ui.fragment

import android.net.Uri
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetbuddy.R
import com.example.budgetbuddy.adapter.ExpenseHistoryAdapter
import com.example.budgetbuddy.databinding.FragmentExpenseHistoryBinding
import com.example.budgetbuddy.ui.dialog.ReceiptDialogFragment
import com.example.budgetbuddy.ui.viewmodel.ExpenseHistoryUiState
import com.example.budgetbuddy.ui.viewmodel.ExpenseHistoryViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ExpenseHistoryFragment : Fragment() {

    private var _binding: FragmentExpenseHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpenseHistoryViewModel by viewModels()
    private lateinit var expenseHistoryAdapter: ExpenseHistoryAdapter

    // Date formatter for the chip
    private val chipDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        // Set initial chip text (could be based on VM state if needed)
        updateChipText(null, null) // Initially show default text
    }

    private fun setupRecyclerView() {
        expenseHistoryAdapter = ExpenseHistoryAdapter(
            context = requireContext(),
            onExpenseClicked = { expenseItem ->
                // TODO: Handle item click - e.g., navigate to expense details
                Toast.makeText(context, "Clicked on ${expenseItem.description}", Toast.LENGTH_SHORT).show()
            },
            onViewReceiptClicked = { receiptUri ->
                // Show the receipt dialog when the button is clicked
                showReceiptDialog(receiptUri)
            }
        )
        binding.expensesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = expenseHistoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.dateRangeChip.setOnClickListener {
            showDateRangePicker()
        }
    }

    private fun showDateRangePicker() {
        // Build the date range picker
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            // Optionally set initial selection
            // .setSelection(Pair(MaterialDatePicker.thisMonthInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds()))
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            // selection is Pair<Long, Long> representing UTC start and end milliseconds
            val startDateMillis = selection.first
            val endDateMillis = selection.second

            // Convert UTC milliseconds to local Date objects
            val startDate = Date(startDateMillis + TimeZone.getDefault().getOffset(startDateMillis))
            val endDate = Date(endDateMillis + TimeZone.getDefault().getOffset(endDateMillis))

            // Update the chip text
            updateChipText(startDate, endDate)

            // Update the ViewModel with the selected range
            viewModel.setDateRange(startDate, endDate)
        }

        // Show the picker
        dateRangePicker.show(parentFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun updateChipText(startDate: Date?, endDate: Date?) {
        if (startDate != null && endDate != null) {
            binding.dateRangeChip.text = "${chipDateFormat.format(startDate)} - ${chipDateFormat.format(endDate)}"
        } else {
            // Reset to default text if dates are null
            binding.dateRangeChip.text = getString(R.string.date_range_last_30_days) // Or "Select Date Range"
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state is ExpenseHistoryUiState.Loading
                    binding.expensesRecyclerView.isVisible = state is ExpenseHistoryUiState.Success
                    binding.emptyStateTextView.isVisible = state is ExpenseHistoryUiState.Empty || state is ExpenseHistoryUiState.Error

                    when (state) {
                        is ExpenseHistoryUiState.Success -> {
                            expenseHistoryAdapter.submitList(state.items)
                        }
                        is ExpenseHistoryUiState.Error -> {
                            binding.emptyStateTextView.text = state.message
                             Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                        is ExpenseHistoryUiState.Loading -> {
                            // Handled by progressBar visibility
                        }
                        is ExpenseHistoryUiState.Empty -> {
                             binding.emptyStateTextView.text = "No expenses found."
                        }
                    }
                }
            }
        }
    }

    // Function to show the receipt dialog
    private fun showReceiptDialog(receiptUri: Uri) {
        val dialogFragment = ReceiptDialogFragment.newInstance(receiptUri)
        dialogFragment.show(parentFragmentManager, ReceiptDialogFragment.TAG)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 