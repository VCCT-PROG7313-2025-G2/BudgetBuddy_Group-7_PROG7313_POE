package com.example.budgetbuddy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.ItemExpenseBinding
import com.example.budgetbuddy.databinding.ItemExpenseDateHeaderBinding
import com.example.budgetbuddy.model.ExpenseListItem
import java.text.NumberFormat
import java.util.Locale

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_ITEM = 1

class ExpenseAdapter(private val onItemClicked: (ExpenseListItem.Expense) -> Unit) :
    ListAdapter<ExpenseListItem, RecyclerView.ViewHolder>(ExpenseDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ExpenseListItem.DateHeader -> ITEM_VIEW_TYPE_HEADER
            is ExpenseListItem.Expense -> ITEM_VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> DateHeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_ITEM -> ExpenseViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DateHeaderViewHolder -> {
                val item = getItem(position) as ExpenseListItem.DateHeader
                holder.bind(item)
            }
            is ExpenseViewHolder -> {
                val item = getItem(position) as ExpenseListItem.Expense
                holder.bind(item, onItemClicked)
            }
        }
    }

    class ExpenseViewHolder private constructor(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExpenseListItem.Expense, onItemClicked: (ExpenseListItem.Expense) -> Unit) {
            binding.categoryIconImageView.setImageResource(item.categoryIconRes)
            binding.categoryNameTextView.text = item.categoryName
            binding.notesTextView.text = item.notes
            binding.notesTextView.visibility = if (item.notes.isNullOrBlank()) View.GONE else View.VISIBLE
            
            // Format currency
            binding.amountTextView.text = "R${String.format("%.2f", item.amount)}"

            binding.receiptIndicatorImageView.visibility = if (item.hasReceipt) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }

        companion object {
            fun from(parent: ViewGroup): ExpenseViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemExpenseBinding.inflate(layoutInflater, parent, false)
                return ExpenseViewHolder(binding)
            }
        }
    }

    class DateHeaderViewHolder private constructor(private val binding: ItemExpenseDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExpenseListItem.DateHeader) {
            binding.dateHeaderTextView.text = item.dateString
        }

        companion object {
            fun from(parent: ViewGroup): DateHeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemExpenseDateHeaderBinding.inflate(layoutInflater, parent, false)
                return DateHeaderViewHolder(binding)
            }
        }
    }
}

class ExpenseDiffCallback : DiffUtil.ItemCallback<ExpenseListItem>() {
    override fun areItemsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean {
        return oldItem == newItem
    }
} 