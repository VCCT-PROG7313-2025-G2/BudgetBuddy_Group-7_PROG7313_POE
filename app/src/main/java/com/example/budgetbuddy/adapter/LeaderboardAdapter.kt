package com.example.budgetbuddy.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.ItemLeaderboardRankBinding
import com.example.budgetbuddy.model.LeaderboardRank
import java.text.NumberFormat
import java.util.*

class LeaderboardAdapter(private var ranks: List<LeaderboardRank>) :
    RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    class LeaderboardViewHolder(val binding: ItemLeaderboardRankBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardRankBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val rank = ranks[position]
        val context = holder.itemView.context
        val rankNumber = position + 1

        holder.binding.rankTextView.text = rankNumber.toString()
        holder.binding.nameTextView.text = rank.name
        val format = NumberFormat.getInstance(Locale.getDefault())
        holder.binding.pointsTextView.text = "${format.format(rank.points)} pts"

        Glide.with(context)
            .load(rank.profileImageRes) // Assuming resource ID for now
            .circleCrop()
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(holder.binding.profileImageView)

        // Highlight the current user? Needs user ID comparison.
        // if (rank.userId == currentUserId) { ... }
    }

    override fun getItemCount() = ranks.size

    // Basic update function
    fun updateData(newRanks: List<LeaderboardRank>) {
        ranks = newRanks
        notifyDataSetChanged()
    }
} 