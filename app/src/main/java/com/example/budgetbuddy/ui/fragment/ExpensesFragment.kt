package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetbuddy.R
import com.example.budgetbuddy.adapter.ExpenseAdapter // Import adapter (will be created)
import com.example.budgetbuddy.databinding.FragmentExpensesBinding
import com.example.budgetbuddy.model.ExpenseListItem // Import model (will be created)
import com.example.budgetbuddy.util.CurrencyConverter
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private lateinit var expenseAdapter: ExpenseAdapter

    // Inject CurrencyConverter for proper currency formatting
    @Inject
    lateinit var currencyConverter: CurrencyConverter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadExpenses() // Load placeholder data

        binding.dateRangeButton.setOnClickListener {
            // TODO: Implement date range selection logic
            Toast.makeText(context, "Date range selection not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            onItemClicked = { expense ->
                // TODO: Handle expense item click (e.g., navigate to details)
                Toast.makeText(context, "Clicked on: ${expense.categoryName}", Toast.LENGTH_SHORT).show()
            },
            currencyConverter = currencyConverter
        )
        binding.expensesRecyclerView.apply {
            adapter = expenseAdapter
            layoutManager = LinearLayoutManager(context)
            // Add item decorations if needed (e.g., dividers)
        }
    }

    private fun loadExpenses() {
        // TODO: Replace with actual data loading from ViewModel/Repository
        val placeholderData = getPlaceholderExpenses()
        expenseAdapter.submitList(placeholderData)
    }

    // --- Placeholder Data Generation --- 
    private fun getPlaceholderExpenses(): List<ExpenseListItem> {
        return listOf(
            ExpenseListItem.DateHeader("Today"),
            ExpenseListItem.Expense(1, "Restaurant", "Lunch with Sam", 24.50, R.drawable.ic_category_food, Date(), true),
            ExpenseListItem.Expense(2, "Transport", "Bus fare", 2.75, R.drawable.ic_category_transport, Date(), false),
            ExpenseListItem.DateHeader("Yesterday"),
            ExpenseListItem.Expense(3, "Shopping", "Groceries", 67.90, R.drawable.ic_category_shopping, Date(System.currentTimeMillis() - 86400000), false),
             ExpenseListItem.Expense(4, "Utilities", "Electricity Bill", 112.15, R.drawable.ic_category_utilities, Date(System.currentTimeMillis() - 86400000), true)
        )
    }
    // --- End Placeholder Data ---

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 