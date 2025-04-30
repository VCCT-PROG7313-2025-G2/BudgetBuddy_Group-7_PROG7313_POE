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
import androidx.core.view.isVisible

@AndroidEntryPoint
class BudgetSetupFragment : Fragment() {

    private var _binding: FragmentBudgetSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BudgetSetupViewModel by viewModels()
    private lateinit var categoryBudgetAdapter: CategoryBudgetAdapter
    private val categoryBudgets = mutableListOf<CategoryBudget>() // Store current category budget data

    // Temporary map to hold placeholder icon resources based on a generated ID
    private val tempIconMap = mapOf(
        "cat_food" to R.drawable.ic_category_food,
        "cat_transport" to R.drawable.ic_category_transport,
        "cat_shopping" to R.drawable.ic_category_shopping,
        "cat_utilities" to R.drawable.ic_category_utilities,
        "cat_other" to R.drawable.ic_category_other
    )

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

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        // Don't show the action bar on pause to prevent it from flashing
    }

    private fun setupClickListeners() {
        binding.addCategoryButton.setOnClickListener {
            // Toggle visibility of the inline add card
            binding.addCategoryInputCard.isVisible = !binding.addCategoryInputCard.isVisible
        }

        binding.saveBudgetButton.setOnClickListener {
            saveBudgetData()
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp() // Use the new backButton ID
        }

        binding.inlineAddButton.setOnClickListener {
            addNewCategoryBudget()
        }
    }

    private fun setupRecyclerView() {
        categoryBudgetAdapter = CategoryBudgetAdapter { categoryBudget, newLimit ->
            // TODO: Handle the updated limit for a specific category
            // Find the item in our local list and update it
            val index = categoryBudgets.indexOfFirst { it.categoryId == categoryBudget.categoryId }
            if (index != -1) {
                categoryBudgets[index].budgetLimit = newLimit
                // Note: Adapter's internal list is handled by submitList.
                // This update is for our local copy if needed for saving.
            }
            println("Updated budget for ${categoryBudget.categoryName}: $newLimit") // Placeholder
        }
        binding.categoryBudgetsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryBudgetAdapter
        }
        // TODO: Load existing category budgets from ViewModel if editing
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

    private fun saveBudgetData() {
        val monthlyBudgetText = binding.monthlyBudgetEditText.text.toString()
        val totalBudget = try {
            if (monthlyBudgetText.isNotBlank()) BigDecimal(monthlyBudgetText) else BigDecimal.ZERO
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid monthly budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Map the current CategoryBudget list (from fragment state) to CategoryBudgetInput for ViewModel
        val categoryBudgetInputs = categoryBudgets.map {
            CategoryBudgetInput(
                categoryName = it.categoryName,
                // Ensure limit is not null, default to ZERO if it is (or handle appropriately)
                limit = it.budgetLimit?.toBigDecimal() ?: BigDecimal.ZERO
            )
        }

        viewModel.saveBudget(totalBudget, categoryBudgetInputs)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // TODO: Handle loading state (e.g., show progress bar)
                    binding.saveBudgetButton.isEnabled = state !is BudgetSetupUiState.Loading

                    when (state) {
                        is BudgetSetupUiState.Success -> {
                            Toast.makeText(context, "Budget saved successfully!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp() // Navigate back after success
                            viewModel.resetState() // Reset state in ViewModel
                        }
                        is BudgetSetupUiState.Error -> {
                            Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        else -> Unit // Idle or Loading
                    }
                }
            }
        }
    }

    private fun addNewCategoryBudget() {
        val name = binding.newCategoryNameEditText.text.toString().trim()
        val amountText = binding.newCategoryAmountEditText.text.toString()

        if (name.isBlank()) {
            Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = try {
            if (amountText.isNotBlank()) BigDecimal(amountText) else BigDecimal.ZERO
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid budget amount", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Implement proper category ID generation or selection
        // For now, using name as a pseudo-ID and picking a placeholder icon
        val categoryId = "cat_${name.lowercase().replace(" ", "_")}" 
        val iconRes = tempIconMap.entries.random().value // Random placeholder icon

        val newCategory = CategoryBudget(
            categoryId = categoryId,
            categoryName = name,
            categoryIconRes = iconRes,
            budgetLimit = amount.toDouble() // Adapter expects Double?
        )

        // Add to local list and update adapter
        categoryBudgets.add(newCategory)
        categoryBudgetAdapter.submitList(categoryBudgets.toList()) // Submit updated copy

        // Clear input fields and hide the card
        binding.newCategoryNameEditText.text = null
        binding.newCategoryAmountEditText.text = null
        binding.addCategoryInputCard.isVisible = false
        Toast.makeText(context, "Category '$name' added", Toast.LENGTH_SHORT).show()
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