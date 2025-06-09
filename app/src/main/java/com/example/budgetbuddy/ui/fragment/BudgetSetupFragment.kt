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
import com.example.budgetbuddy.ui.viewmodel.FirebaseBudgetSetupUiState
import com.example.budgetbuddy.ui.viewmodel.FirebaseBudgetSetupViewModel
import com.example.budgetbuddy.ui.viewmodel.CategoryBudgetInputUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigDecimal
import androidx.core.view.isVisible
import android.text.TextWatcher
import android.text.Editable

@AndroidEntryPoint
class BudgetSetupFragment : Fragment() {

    private var _binding: FragmentBudgetSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FirebaseBudgetSetupViewModel by viewModels()
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
        setupBudgetValidation()
        loadBudgetData() // Load existing or default budgets
        
        // Try to load existing budget for current month
        loadExistingBudgetIfExists()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        // Don't show the action bar on pause to prevent it from flashing
    }

    private fun setupBudgetValidation() {
        binding.monthlyBudgetEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val budgetText = s?.toString() ?: ""
                
                if (budgetText.isNotBlank()) {
                    try {
                        val budgetAmount = BigDecimal(budgetText)
                        val minimumBudget = com.example.budgetbuddy.util.Constants.Budget.MINIMUM_BUDGET_AMOUNT
                        
                        when {
                            budgetAmount <= BigDecimal.ZERO -> {
                                binding.monthlyBudgetInputLayout.error = "Budget must be greater than zero"
                                binding.monthlyBudgetInputLayout.helperText = null
                            }
                            budgetAmount < minimumBudget -> {
                                binding.monthlyBudgetInputLayout.error = "Budget must be at least $$minimumBudget"
                                binding.monthlyBudgetInputLayout.helperText = null
                            }
                            else -> {
                                binding.monthlyBudgetInputLayout.error = null
                                binding.monthlyBudgetInputLayout.helperText = "Minimum budget: $$minimumBudget"
                            }
                        }
                    } catch (e: NumberFormatException) {
                        binding.monthlyBudgetInputLayout.error = "Please enter a valid amount"
                        binding.monthlyBudgetInputLayout.helperText = null
                    }
                } else {
                    binding.monthlyBudgetInputLayout.error = null
                    val minimumBudget = com.example.budgetbuddy.util.Constants.Budget.MINIMUM_BUDGET_AMOUNT
                    binding.monthlyBudgetInputLayout.helperText = "Minimum budget: $$minimumBudget"
                }
            }
        })
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
            // Update the ViewModel when user changes category amount
            android.util.Log.d("BudgetSetupFragment", "Category ${categoryBudget.categoryName} updated to: $newLimit")
            
            val amount = when {
                newLimit == null -> BigDecimal.ZERO
                newLimit <= 0.0 -> BigDecimal.ZERO
                else -> try {
                    BigDecimal(newLimit.toString())
                } catch (e: NumberFormatException) {
                    android.util.Log.e("BudgetSetupFragment", "Invalid amount format: $newLimit", e)
                    BigDecimal.ZERO
                }
            }
            
            // Update the ViewModel - this will trigger observers to update the UI
            viewModel.updateCategoryAllocation(categoryBudget.categoryName, amount)
            
            // Also update our local list for consistency
            val index = categoryBudgets.indexOfFirst { it.categoryId == categoryBudget.categoryId }
            if (index != -1) {
                categoryBudgets[index].budgetLimit = newLimit
            }
        }
        binding.categoryBudgetsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryBudgetAdapter
        }
        // TODO: Load existing category budgets from ViewModel if editing
    }

    private fun loadBudgetData() {
        // Load data from ViewModel instead of placeholder data
        // The actual data will come from observeViewModel() observers
        // Just initialize empty state - data will be populated by observers
        categoryBudgets.clear()
        categoryBudgetAdapter.submitList(emptyList())
    }

    private fun saveBudgetData() {
        android.util.Log.d("BudgetSetupFragment", "=== Starting saveBudgetData ===")
        
        // Disable button to prevent multiple clicks
        binding.saveBudgetButton.isEnabled = false
        
        try {
            val monthlyBudgetText = binding.monthlyBudgetEditText.text.toString()
            android.util.Log.d("BudgetSetupFragment", "Monthly budget text: '$monthlyBudgetText'")
            
            val totalBudget = try {
                if (monthlyBudgetText.isNotBlank()) BigDecimal(monthlyBudgetText) else BigDecimal.ZERO
            } catch (e: NumberFormatException) {
                android.util.Log.e("BudgetSetupFragment", "Invalid budget format", e)
                Toast.makeText(context, "Invalid monthly budget amount", Toast.LENGTH_SHORT).show()
                binding.saveBudgetButton.isEnabled = true
                return
            }
            android.util.Log.d("BudgetSetupFragment", "Parsed total budget: $totalBudget")

            // Validate minimum budget
            if (totalBudget <= BigDecimal.ZERO) {
                android.util.Log.w("BudgetSetupFragment", "Budget is zero or negative")
                Toast.makeText(context, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
                binding.saveBudgetButton.isEnabled = true
                return
            } else if (totalBudget < com.example.budgetbuddy.util.Constants.Budget.MINIMUM_BUDGET_AMOUNT) {
                android.util.Log.w("BudgetSetupFragment", "Budget is below minimum threshold")
                Toast.makeText(context, "Budget must be at least $${com.example.budgetbuddy.util.Constants.Budget.MINIMUM_BUDGET_AMOUNT}", Toast.LENGTH_LONG).show()
                binding.saveBudgetButton.isEnabled = true
                return
            }

            android.util.Log.d("BudgetSetupFragment", "Setting total budget in ViewModel: $totalBudget")
            // First set the total budget
            viewModel.setTotalBudget(totalBudget)
            
            android.util.Log.d("BudgetSetupFragment", "Processing ${categoryBudgets.size} categories")
            // Add each category to the ViewModel
            categoryBudgets.forEachIndexed { index, categoryBudget ->
                try {
                    android.util.Log.d("BudgetSetupFragment", "Processing category $index: '${categoryBudget.categoryName}', budgetLimit: '${categoryBudget.budgetLimit}'")
                    
                    val amount = when {
                        categoryBudget.budgetLimit == null -> {
                            android.util.Log.d("BudgetSetupFragment", "Category ${categoryBudget.categoryName} has null budgetLimit, using ZERO")
                            BigDecimal.ZERO
                        }
                        categoryBudget.budgetLimit!! <= 0.0 -> {
                            android.util.Log.d("BudgetSetupFragment", "Category ${categoryBudget.categoryName} has zero/negative budgetLimit, using ZERO")
                            BigDecimal.ZERO
                        }
                        else -> {
                            try {
                                val bdAmount = BigDecimal(categoryBudget.budgetLimit!!.toString())
                                android.util.Log.d("BudgetSetupFragment", "Category ${categoryBudget.categoryName} converted to BigDecimal: $bdAmount")
                                bdAmount
                            } catch (nfe: NumberFormatException) {
                                android.util.Log.e("BudgetSetupFragment", "Failed to convert budgetLimit '${categoryBudget.budgetLimit}' to BigDecimal", nfe)
                                BigDecimal.ZERO
                            }
                        }
                    }
                    
                                android.util.Log.d("BudgetSetupFragment", "========== SETTING CATEGORY ==========")
            android.util.Log.d("BudgetSetupFragment", "Category: ${categoryBudget.categoryName}")
            android.util.Log.d("BudgetSetupFragment", "Raw budgetLimit: ${categoryBudget.budgetLimit}")
            android.util.Log.d("BudgetSetupFragment", "Converted amount: $amount")
            android.util.Log.d("BudgetSetupFragment", "========================================")
            viewModel.updateCategoryAllocation(categoryBudget.categoryName, amount)
                } catch (e: Exception) {
                    android.util.Log.e("BudgetSetupFragment", "Error processing category ${categoryBudget.categoryName}", e)
                    // Continue with other categories even if one fails
                }
            }

            android.util.Log.d("BudgetSetupFragment", "Calling viewModel.saveBudget()")
            // Save the budget
            viewModel.saveBudget()
            
        } catch (e: Exception) {
            android.util.Log.e("BudgetSetupFragment", "Error in saveBudgetData", e)
            android.util.Log.e("BudgetSetupFragment", "Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("BudgetSetupFragment", "Exception message: '${e.message}'")
            android.util.Log.e("BudgetSetupFragment", "Exception cause: '${e.cause}'")
            
            val errorMessage = when {
                e.message != null -> "Save error: ${e.message}"
                e.cause?.message != null -> "Save error: ${e.cause?.message}"
                else -> "Save error: ${e.javaClass.simpleName} - Unknown error occurred"
            }
            
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            binding.saveBudgetButton.isEnabled = true
        }
    }

    private fun observeViewModel() {
        // Use repeatOnLifecycle to properly handle lifecycle changes
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe UI state for loading/success/error
                launch {
                    try {
                        viewModel.uiState.collect { state ->
                            if (!isAdded || _binding == null) {
                                android.util.Log.w("BudgetSetupFragment", "Fragment not active during UI state update")
                                return@collect
                            }
                            
                            binding.saveBudgetButton.isEnabled = state !is FirebaseBudgetSetupUiState.Loading

                            when (state) {
                                is FirebaseBudgetSetupUiState.Success -> {
                                    android.util.Log.d("BudgetSetupFragment", "Budget save success received")
                                    Toast.makeText(context, "Budget saved successfully!", Toast.LENGTH_SHORT).show()
                                    
                                    // Add a small delay before navigation to ensure UI updates complete
                                    kotlinx.coroutines.delay(500)
                                    
                                    if (isAdded && _binding != null) {
                                        try {
                                            findNavController().navigateUp()
                                        } catch (e: Exception) {
                                            android.util.Log.e("BudgetSetupFragment", "Navigation error", e)
                                        }
                                    }
                                }
                                is FirebaseBudgetSetupUiState.Error -> {
                                    android.util.Log.e("BudgetSetupFragment", "ViewModel error: '${state.message}'")
                                    
                                    val errorMessage = when {
                                        state.message.isNotBlank() -> "Error: ${state.message}"
                                        else -> "Error: Unknown error from ViewModel"
                                    }
                                    
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    binding.saveBudgetButton.isEnabled = true
                                }
                                is FirebaseBudgetSetupUiState.Loading -> {
                                    android.util.Log.d("BudgetSetupFragment", "Budget save in progress...")
                                    // Keep button disabled during loading
                                }
                                is FirebaseBudgetSetupUiState.Idle -> {
                                    binding.saveBudgetButton.isEnabled = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BudgetSetupFragment", "Error in UI state observer", e)
                    }
                }
            }
        }

        // Observe categories in a separate coroutine to prevent interference
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    viewModel.categories.collect { categoryInputStates ->
                        if (!isAdded || _binding == null) {
                            android.util.Log.w("BudgetSetupFragment", "Fragment not active during category update")
                            return@collect
                        }
                        
                        android.util.Log.d("BudgetSetupFragment", "Received ${categoryInputStates.size} categories from ViewModel")
                        
                        // Convert ViewModel states to UI models
                        val uiCategories = categoryInputStates.mapIndexed { index, categoryState ->
                            CategoryBudget(
                                categoryId = "category_$index",
                                categoryName = categoryState.categoryName,
                                categoryIconRes = getCategoryIcon(categoryState.categoryName),
                                budgetLimit = if (categoryState.allocatedAmount > BigDecimal.ZERO) 
                                    categoryState.allocatedAmount.toDouble() else null
                            )
                        }
                        
                        // Update the list
                        categoryBudgets.clear()
                        categoryBudgets.addAll(uiCategories)
                        
                        // Notify adapter of changes
                        try {
                            categoryBudgetAdapter.submitList(categoryBudgets.toList()) {
                                android.util.Log.d("BudgetSetupFragment", "Category list submitted to adapter")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BudgetSetupFragment", "Error updating adapter", e)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BudgetSetupFragment", "Error in category observer", e)
                }
            }
        }

        // Observe total budget in a separate coroutine
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    viewModel.totalBudget.collect { totalBudget ->
                        if (!isAdded || _binding == null) {
                            android.util.Log.w("BudgetSetupFragment", "Fragment not active during budget update")
                            return@collect
                        }
                        
                        android.util.Log.d("BudgetSetupFragment", "Total budget updated: $totalBudget")
                        
                        // Update the budget input field
                        if (totalBudget > BigDecimal.ZERO) {
                            binding.monthlyBudgetEditText.setText(totalBudget.toString())
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BudgetSetupFragment", "Error observing total budget", e)
                }
            }
        }
    }
    
    // Helper method to get category icon
    private fun getCategoryIcon(categoryName: String?): Int {
        return try {
            when (categoryName?.lowercase()?.trim()) {
                "food & dining", "food" -> R.drawable.ic_category_food
                "transportation", "transport" -> R.drawable.ic_category_transport
                "shopping" -> R.drawable.ic_category_shopping
                "bills & utilities", "utilities" -> R.drawable.ic_category_utilities
                "entertainment" -> R.drawable.ic_category_other
                "healthcare", "health" -> R.drawable.ic_category_other
                "education" -> R.drawable.ic_category_other
                "travel" -> R.drawable.ic_category_other
                else -> R.drawable.ic_category_other
            }
        } catch (e: Exception) {
            android.util.Log.e("BudgetSetupFragment", "Error getting category icon for: $categoryName", e)
            R.drawable.ic_category_other
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

        // Optional: Validate category amount against a reasonable minimum
        if (amount > BigDecimal.ZERO && amount < com.example.budgetbuddy.util.Constants.Budget.MINIMUM_CATEGORY_AMOUNT) {
            Toast.makeText(context, "Category budget should be at least $${com.example.budgetbuddy.util.Constants.Budget.MINIMUM_CATEGORY_AMOUNT}", Toast.LENGTH_SHORT).show()
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

    private fun loadExistingBudgetIfExists() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentUserId = viewModel.getCurrentUserId()
                val currentMonthYear = viewModel.getCurrentMonthYear()
                
                android.util.Log.d("BudgetSetupFragment", "Loading existing budget for $currentUserId, month: $currentMonthYear")
                
                // This will trigger the ViewModel to load existing data if it exists
                viewModel.checkAndLoadExistingBudget(currentMonthYear)
            } catch (e: Exception) {
                android.util.Log.e("BudgetSetupFragment", "Error loading existing budget", e)
            }
        }
    }

    override fun onDestroyView() {
        android.util.Log.d("BudgetSetupFragment", "onDestroyView called")
        super.onDestroyView()
        _binding = null
    }
    
    override fun onDetach() {
        android.util.Log.d("BudgetSetupFragment", "onDetach called")
        super.onDetach()
    }
    
    override fun onStop() {
        android.util.Log.d("BudgetSetupFragment", "onStop called")
        super.onStop()
    }
} 