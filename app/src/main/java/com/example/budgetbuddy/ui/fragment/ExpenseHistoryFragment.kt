package com.example.budgetbuddy.ui.fragment

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetbuddy.adapter.ExpenseHistoryAdapter
import com.example.budgetbuddy.adapter.HistoryListItem
import com.example.budgetbuddy.databinding.FragmentExpenseHistoryBinding
import com.example.budgetbuddy.model.ExpenseItemUi
import com.example.budgetbuddy.ui.viewmodel.FirebaseExpenseHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ExpenseHistoryFragment : Fragment() {

    private var _binding: FragmentExpenseHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FirebaseExpenseHistoryViewModel by viewModels()
    private lateinit var expenseHistoryAdapter: ExpenseHistoryAdapter

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

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
        setupStatusBar()
    }

    private fun setupStatusBar() {
        // Force status bar to be white/transparent
        activity?.window?.apply {
            statusBarColor = android.graphics.Color.WHITE
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        
        // Also hide the action bar completely for this fragment
        (activity as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.hide()
    }

    private fun setupRecyclerView() {
        expenseHistoryAdapter = ExpenseHistoryAdapter(
            context = requireContext(),
            onExpenseClicked = { expense ->
                showExpenseDetails(expense)
            },
            onViewReceiptClicked = { uri ->
                viewReceipt(uri)
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
            showDateRangeDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    Log.d("ExpenseHistoryFragment", "UI State: isLoading=${state.isLoading}, expenses=${state.expenses.size}")

                    // Update loading indicator
                    binding.progressBar.isVisible = state.isLoading
                    
                    // Update empty state
                    binding.emptyStateTextView.isVisible = !state.isLoading && state.expenses.isEmpty()
                    
                    // Update date range chip text
                    val startDate = dateFormat.format(state.startDate)
                    val endDate = dateFormat.format(state.endDate)
                    binding.dateRangeChip.text = "$startDate - $endDate"

                    // Update recycler view with grouped expenses
                    if (!state.isLoading) {
                        val groupedItems = groupExpensesByDate(state.expenses)
                        expenseHistoryAdapter.submitList(groupedItems)
                        Log.d("ExpenseHistoryFragment", "Submitted ${groupedItems.size} items to adapter")
                    }

                    // Handle errors
                    state.error?.let { error ->
                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun groupExpensesByDate(expenses: List<ExpenseItemUi>): List<HistoryListItem> {
        val items = mutableListOf<HistoryListItem>()
        var currentDate: String? = null

        expenses.forEach { expense ->
            val expenseDate = dateFormat.format(expense.date)
            
            // Add date header if this is a new date
            if (currentDate != expenseDate) {
                items.add(HistoryListItem.DateHeader(expenseDate))
                currentDate = expenseDate
            }
            
            // Add expense item
            items.add(HistoryListItem.ExpenseEntry(expense))
        }

                return items
    }

    private fun showDateRangeDialog() {
        val calendar = Calendar.getInstance()
        
        // Start date picker
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val startDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                // End date picker
                DatePickerDialog(
                    requireContext(),
                    { _, endYear, endMonth, endDayOfMonth ->
                        val endDate = Calendar.getInstance().apply {
                            set(endYear, endMonth, endDayOfMonth, 23, 59, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.time

                        if (startDate.before(endDate) || startDate == endDate) {
                            viewModel.setDateRange(startDate, endDate)
                        } else {
                            Toast.makeText(context, "Start date must be before end date", Toast.LENGTH_SHORT).show()
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setTitle("Select End Date")
                    show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Select Start Date")
            show()
        }
    }

    private fun showExpenseDetails(expense: ExpenseItemUi) {
        // Create a detailed view dialog or navigate to detail fragment
        val details = buildString {
            append("Category: ${expense.category}\n")
            append("Amount: R${String.format("%.2f", expense.amount)}\n")
            append("Date: ${dateFormat.format(expense.date)}\n")
            if (expense.description.isNotEmpty()) {
                append("Description: ${expense.description}\n")
            }
            if (expense.receiptPath != null) {
                append("Receipt: Available")
            } else {
                append("Receipt: Not available")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Expense Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .apply {
                if (expense.receiptPath != null) {
                    setNeutralButton("View Receipt") { _, _ ->
                        viewReceipt(Uri.parse(expense.receiptPath))
                    }
                }
            }
            .show()
    }

    private fun viewReceipt(uri: Uri) {
        try {
            // Show receipt in a custom dialog fragment for better in-app experience
            val receiptDialog = com.example.budgetbuddy.ui.dialog.ReceiptDialogFragment.newInstance(uri)
            receiptDialog.show(childFragmentManager, "ReceiptDialog")
            
        } catch (e: Exception) {
            Log.e("ExpenseHistoryFragment", "Error viewing receipt", e)
            // Fallback to external intent if dialog fails
            try {
            val intent = if (uri.scheme == "http" || uri.scheme == "https") {
                Intent(Intent.ACTION_VIEW, uri)
            } else {
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, "No app found to view images", Toast.LENGTH_SHORT).show()
            }
            } catch (e2: Exception) {
                Log.e("ExpenseHistoryFragment", "Error with fallback intent", e2)
            Toast.makeText(context, "Error opening receipt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        
        // Don't manually show action bar - let MainActivity handle it
    }
} 