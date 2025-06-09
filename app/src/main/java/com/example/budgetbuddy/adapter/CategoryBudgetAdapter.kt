package com.example.budgetbuddy.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetbuddy.databinding.ItemCategoryBudgetBinding
import com.example.budgetbuddy.model.CategoryBudget
import com.example.budgetbuddy.util.CurrencyConverter
import java.text.NumberFormat
import java.util.Locale

class CategoryBudgetAdapter(
    private val onBudgetChanged: (CategoryBudget, Double?) -> Unit,
    private val currencyConverter: CurrencyConverter
) : ListAdapter<CategoryBudget, CategoryBudgetAdapter.CategoryBudgetViewHolder>(CategoryBudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryBudgetViewHolder {
        return CategoryBudgetViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: CategoryBudgetViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onBudgetChanged, currencyConverter)
    }

    // Function to get the current state of budgets from the adapter's list
    fun getCurrentBudgets(): List<CategoryBudget> {
        // The list managed by ListAdapter reflects the current items
        return currentList
    }

    class CategoryBudgetViewHolder private constructor(private val binding: ItemCategoryBudgetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Keep track of the current item to update its limit
        private var currentItem: CategoryBudget? = null

        fun bind(item: CategoryBudget, onBudgetChanged: (CategoryBudget, Double?) -> Unit, currencyConverter: CurrencyConverter) {
            currentItem = item
            binding.categoryIconImageView.setImageResource(item.categoryIconRes)
            binding.categoryNameTextView.text = item.categoryName
            
            // Set currency symbol as prefix
            binding.budgetAmountInputLayout.prefixText = currencyConverter.getCurrencySymbol()
            
            // Set initial value without triggering listener
            val initialLimit = item.budgetLimit?.let { String.format(Locale.US, "%.2f", it) } ?: ""
            if (binding.budgetAmountEditText.text.toString() != initialLimit) {
                binding.budgetAmountEditText.setText(initialLimit)
            }

            // Listener to update budget when text changes
            // Remove previous listener first to avoid issues during binding
            binding.budgetAmountEditText.removeTextChangedListener(textWatcher)
            binding.budgetAmountEditText.addTextChangedListener(textWatcher)

            // Add a focus change listener to notify the fragment when editing is done
            // This helps capture the last entered value if focus moves away
            binding.budgetAmountEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    notifyBudgetChange(onBudgetChanged)
                }
            }
        }

        private val textWatcher = binding.budgetAmountEditText.doAfterTextChanged { text ->
             // No need to notify immediately on every keystroke, use focus change
             // Optionally add input validation feedback here
        }

        private fun notifyBudgetChange(onBudgetChanged: (CategoryBudget, Double?) -> Unit) {
            currentItem?.let { item ->
                val newLimit = binding.budgetAmountEditText.text.toString().toDoubleOrNull()
                if (item.budgetLimit != newLimit) { // Only notify if changed
                    item.budgetLimit = newLimit // Update the item's internal state
                    onBudgetChanged(item, newLimit)
                }
            }
        }

        companion object {
            fun from(parent: ViewGroup): CategoryBudgetViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemCategoryBudgetBinding.inflate(layoutInflater, parent, false)
                return CategoryBudgetViewHolder(binding)
            }
        }
    }
}

class CategoryBudgetDiffCallback : DiffUtil.ItemCallback<CategoryBudget>() {
    override fun areItemsTheSame(oldItem: CategoryBudget, newItem: CategoryBudget): Boolean {
        return oldItem.categoryId == newItem.categoryId
    }

    override fun areContentsTheSame(oldItem: CategoryBudget, newItem: CategoryBudget): Boolean {
        return oldItem == newItem
    }
} 