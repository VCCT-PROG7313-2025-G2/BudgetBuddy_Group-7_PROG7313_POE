package com.example.budgetbuddy.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetbuddy.databinding.ItemAchievementBinding
import com.example.budgetbuddy.model.Achievement

class AchievementAdapter(private val onItemClicked: (Achievement) -> Unit) :
    ListAdapter<Achievement, AchievementAdapter.AchievementViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        return AchievementViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClicked)
    }

    class AchievementViewHolder private constructor(private val binding: ItemAchievementBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Achievement, onItemClicked: (Achievement) -> Unit) {
            binding.achievementIconImageView.setImageResource(item.iconRes)
            binding.achievementNameTextView.text = item.name
            binding.achievementDescTextView.text = item.description
            
            // Adjust alpha for locked achievements
            binding.root.alpha = if (item.isUnlocked) 1.0f else 0.5f

            // TODO: Handle progress bar visibility and value if needed
            // binding.achievementProgressBar.visibility = if (item.progress != null) View.VISIBLE else View.GONE
            // item.progress?.let { binding.achievementProgressBar.progress = it }
            // item.progressMax?.let { binding.achievementProgressBar.max = it }

            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }

        companion object {
            fun from(parent: ViewGroup): AchievementViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemAchievementBinding.inflate(layoutInflater, parent, false)
                return AchievementViewHolder(binding)
            }
        }
    }
}

class AchievementDiffCallback : DiffUtil.ItemCallback<Achievement>() {
    override fun areItemsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Achievement, newItem: Achievement): Boolean {
        return oldItem == newItem
    }
} 