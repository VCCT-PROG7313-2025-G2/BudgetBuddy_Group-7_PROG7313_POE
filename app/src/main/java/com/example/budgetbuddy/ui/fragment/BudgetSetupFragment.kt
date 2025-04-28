package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgetbuddy.R
import com.example.budgetbuddy.adapter.CategoryBudgetAdapter
import com.example.budgetbuddy.databinding.FragmentBudgetSetupBinding
import com.example.budgetbuddy.model.CategoryBudget
import com.example.budgetbuddy.ui.viewmodel.BudgetSetupUiState
import com.example.budgetbuddy.ui.viewmodel.BudgetSetupViewModel
import com.example.budgetbuddy.ui.viewmodel.CategoryBudgetInput
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigDecimal

@AndroidEntryPoint
class BudgetSetupFragment : Fragment() {

    private var _binding: FragmentBudgetSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetSetupViewModel by viewModels()
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
        setupClickListeners()
        observeViewModel()
        setupRecyclerView()
        loadBudgetData() // Load existing or default budgets
    }

    private fun setupClickListeners() {
        binding.saveBudgetButton.setOnClickListener {
            saveBudget()
        }
        // TODO: Add listener for 'Add Category' button if you have one
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
        val overallBudgetText = binding.monthlyBudgetEditText.text.toString()
        val overallBudget = try {
            BigDecimal(overallBudgetText)
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid overall budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the current state of budgets from the adapter
        val currentBudgetsFromAdapter: List<CategoryBudget> = categoryBudgetAdapter.getCurrentBudgets()

        // Convert CategoryBudget (from adapter) to CategoryBudgetInput (for ViewModel)
        val categoryInputs: List<CategoryBudgetInput> = currentBudgetsFromAdapter.mapNotNull { categoryBudget ->
            // Use BigDecimal for ViewModel, handle null limit by defaulting to ZERO
            val limit = categoryBudget.budgetLimit?.toBigDecimal() ?: BigDecimal.ZERO
            // Only include categories where a limit > 0 has been entered? Or send all?
            // Let's send all defined categories and let ViewModel/Repo filter if needed.
            CategoryBudgetInput(
                categoryName = categoryBudget.categoryName, 
                limit = limit
            )
        }

        viewModel.saveBudget(overallBudget, categoryInputs)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.saveBudgetButton.isEnabled = state !is BudgetSetupUiState.Loading
                    // TODO: Show loading indicator

                    when (state) {
                        is BudgetSetupUiState.Success -> {
                            Toast.makeText(context, "Budget saved successfully!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp() // Go back after saving
                            viewModel.resetState() // Reset state
                        }
                        is BudgetSetupUiState.Error -> {
                            Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetState() // Allow retry
                        }
                        else -> Unit // Idle or Loading
                    }
                }
            }
        }
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