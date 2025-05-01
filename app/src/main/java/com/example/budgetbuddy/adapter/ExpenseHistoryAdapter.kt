package com.example.budgetbuddy.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.ItemDateHeaderBinding
import com.example.budgetbuddy.databinding.ItemExpenseHistoryBinding
import com.example.budgetbuddy.model.ExpenseItemUi
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Sealed class to represent different item types in the list
sealed class HistoryListItem {
    data class DateHeader(val dateText: String) : HistoryListItem()
    data class ExpenseEntry(val expense: ExpenseItemUi) : HistoryListItem()
}

class ExpenseHistoryAdapter(
    private val context: Context,
    private val onExpenseClicked: (ExpenseItemUi) -> Unit,
    private val onViewReceiptClicked: (Uri) -> Unit
) :
    ListAdapter<HistoryListItem, RecyclerView.ViewHolder>(HistoryDiffCallback()) {

    private val VIEW_TYPE_DATE_HEADER = 0
    private val VIEW_TYPE_EXPENSE = 1

    // Map to hold category icons (similar to ExpensesFragment)
    private val categoryIconMap = mapOf(
        "Food & Dining" to R.drawable.ic_category_food,
        "Transport" to R.drawable.ic_category_transport,
        "Shopping" to R.drawable.ic_category_shopping,
        "Utilities" to R.drawable.ic_category_utilities,
        //"Entertainment" to R.drawable.ic_category_entertainment,
       // "Health" to R.drawable.ic_category_health,
       // "Education" to R.drawable.ic_category_education,
        "Other" to R.drawable.ic_category_other
        // Add more mappings as needed
    )

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoryListItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is HistoryListItem.ExpenseEntry -> VIEW_TYPE_EXPENSE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(inflater, parent, false)
                DateHeaderViewHolder(binding)
            }
            VIEW_TYPE_EXPENSE -> {
                val binding = ItemExpenseHistoryBinding.inflate(inflater, parent, false)
                ExpenseViewHolder(binding, onExpenseClicked, onViewReceiptClicked)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistoryListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is HistoryListItem.ExpenseEntry -> (holder as ExpenseViewHolder).bind(item.expense, context, categoryIconMap)
        }
    }

    // --- ViewHolders --- 

    class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryListItem.DateHeader) {
            binding.dateHeaderTextView.text = item.dateText
        }
    }

    class ExpenseViewHolder(
        private val binding: ItemExpenseHistoryBinding,
        private val onExpenseClicked: (ExpenseItemUi) -> Unit,
        private val onViewReceiptClicked: (Uri) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: ExpenseItemUi, context: Context, iconMap: Map<String, Int>) {
            binding.categoryNameTextView.text = expense.category
            binding.descriptionTextView.text = expense.description.ifEmpty { "-" }
            binding.amountTextView.text = String.format(Locale.getDefault(), "-$%.2f", expense.amount)
            // Set icon
            val iconRes = iconMap[expense.category] ?: R.drawable.ic_category_other // Default icon
            binding.categoryIconImageView.setImageResource(iconRes)

            // Handle View Receipt Button
            if (expense.receiptPath != null) {
                binding.viewReceiptButton.isVisible = true
                binding.viewReceiptButton.setOnClickListener {
                    val file = File(expense.receiptPath)
                    if(file.exists()){
                        onViewReceiptClicked(Uri.fromFile(file))
                    }
                }
            } else {
                binding.viewReceiptButton.isVisible = false
                binding.viewReceiptButton.setOnClickListener(null)
            }

            // Click listener for the whole item
            itemView.setOnClickListener {
                onExpenseClicked(expense)
            }
        }
    }

    // --- DiffUtil --- 

    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryListItem>() {
        override fun areItemsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem): Boolean {
            return when {
                oldItem is HistoryListItem.DateHeader && newItem is HistoryListItem.DateHeader -> oldItem.dateText == newItem.dateText
                oldItem is HistoryListItem.ExpenseEntry && newItem is HistoryListItem.ExpenseEntry -> oldItem.expense.id == newItem.expense.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem): Boolean {
             return when {
                oldItem is HistoryListItem.DateHeader && newItem is HistoryListItem.DateHeader -> oldItem == newItem // DateHeaders are simple data classes
                oldItem is HistoryListItem.ExpenseEntry && newItem is HistoryListItem.ExpenseEntry -> oldItem.expense == newItem.expense // Compare ExpenseItemUi contents
                else -> false
            }
        }
    }
} 