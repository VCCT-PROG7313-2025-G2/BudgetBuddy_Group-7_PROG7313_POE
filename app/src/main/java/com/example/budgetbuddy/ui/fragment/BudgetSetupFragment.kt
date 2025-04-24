package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetbuddy.R
import com.example.budgetbuddy.adapter.CategoryBudgetAdapter
import com.example.budgetbuddy.databinding.FragmentBudgetSetupBinding
import com.example.budgetbuddy.model.CategoryBudget
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BudgetSetupFragment : Fragment() {

    private var _binding: FragmentBudgetSetupBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryBudgetAdapter: CategoryBudgetAdapter
    private val categoryBudgets = mutableListOf<CategoryBudget>() // Store current category budget data

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadBudgetData() // Load existing or default budgets

        binding.saveBudgetButton.setOnClickListener {
            saveBudget()
        }
    }

    private fun setupRecyclerView() {
        categoryBudgetAdapter = CategoryBudgetAdapter { categoryBudget, newLimit ->
            // Handle the updated limit for a specific category
            // Find the item in our local list and update it (adapter handles its own update)
             categoryBudgets.find { it.categoryId == categoryBudget.categoryId }?.budgetLimit = newLimit
             println("Updated budget for ${categoryBudget.categoryName}: $newLimit") // Placeholder
        }
        binding.categoryBudgetsRecyclerView.apply {
            adapter = categoryBudgetAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun loadBudgetData() {
        // TODO: Load existing budget data from ViewModel/Repository
        // For now, load placeholder categories
        val placeholderMonthlyBudget = 2000.0 // Example
        binding.monthlyBudgetEditText.setText(String.format("%.2f", placeholderMonthlyBudget))

        categoryBudgets.clear()
        categoryBudgets.addAll(getPlaceholderCategoryBudgets())
        categoryBudgetAdapter.submitList(categoryBudgets.toList()) // Submit a copy to ListAdapter
    }

    private fun saveBudget() {
        val monthlyBudgetStr = binding.monthlyBudgetEditText.text.toString()
        val monthlyBudget = monthlyBudgetStr.toDoubleOrNull()

        if (monthlyBudget == null || monthlyBudget <= 0) {
            Toast.makeText(context, "Please enter a valid overall monthly budget.", Toast.LENGTH_SHORT).show()
            return
        }

        // Ensure all category budgets are collected from the adapter's current state if needed,
        // although the onBudgetChanged callback should keep `categoryBudgets` list up-to-date.

        // TODO: Implement actual saving logic using ViewModel/Repository
        println("Saving Budget:")
        println(" Monthly Budget: $monthlyBudget")
        categoryBudgets.forEach {
            println("  ${it.categoryName}: ${it.budgetLimit ?: "No Limit"}")
        }

        Toast.makeText(context, "Budget Saved (Not implemented)", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp() // Navigate back
    }

    // --- Placeholder Data Generation --- 
    private fun getPlaceholderCategoryBudgets(): List<CategoryBudget> {
        // Use the same icons as in ExpensesFragment for consistency
        return listOf(
            CategoryBudget("food", "Food & Dining", R.drawable.ic_category_food, 500.0),
            CategoryBudget("transport", "Transport", R.drawable.ic_category_transport, 150.0),
            CategoryBudget("shopping", "Shopping", R.drawable.ic_category_shopping, 300.0),
            CategoryBudget("utilities", "Utilities", R.drawable.ic_category_utilities, 200.0),
            CategoryBudget("other", "Other", R.drawable.ic_category_other, null) // Example with no limit
        )
    }
    // --- End Placeholder Data ---

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 