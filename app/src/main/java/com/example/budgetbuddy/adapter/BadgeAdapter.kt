package com.example.budgetbuddy.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetbuddy.databinding.ItemBadgeBinding
import com.example.budgetbuddy.model.Badge

class BadgeAdapter(private var badges: List<Badge>) :
    RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    class BadgeViewHolder(val binding: ItemBadgeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]
        holder.binding.badgeImageView.setImageResource(badge.iconResId)
        holder.binding.badgeNameTextView.text = badge.name
        // Add onClickListener if badges should be interactive
    }

    override fun getItemCount() = badges.size

    // Basic update function (replace with DiffUtil later for performance)
    fun updateData(newBadges: List<Badge>) {
        badges = newBadges
        notifyDataSetChanged()
    }
} 