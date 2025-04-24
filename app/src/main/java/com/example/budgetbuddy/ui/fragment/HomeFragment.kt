package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Placeholder
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Populate UI with actual data (profile name, balance, charts, lists)
        binding.profileNameTextView.text = "Alex Johnson" // Placeholder
        binding.balanceAmountTextView.text = "$3,452" // Placeholder

        binding.addExpenseButton.setOnClickListener {
            // Navigate to New Expense screen (Screen 5) using the defined action
            findNavController().navigate(R.id.action_homeFragment_to_newExpenseFragment)
            // Toast.makeText(context, "Add Expense Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        binding.notificationsButton.setOnClickListener {
             // TODO: Handle notifications click
            Toast.makeText(context, "Notifications Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }
        
        binding.profileImageView.setOnClickListener {
             // Navigate to Profile screen
             findNavController().navigate(R.id.profileFragment)
        }
        
         binding.profileNameTextView.setOnClickListener {
             // Navigate to Profile screen
             findNavController().navigate(R.id.profileFragment)
        }

        // TODO: Set up RecyclerView for Budget Categories
        binding.budgetCategoriesLabelTextView.setOnClickListener { // Temp navigation trigger
             findNavController().navigate(R.id.action_homeFragment_to_budgetSetupFragment)
        }

        // TODO: Set up Chart for Spending Trend
        binding.spendingTrendChartView.setOnClickListener { // Temp navigation trigger
            findNavController().navigate(R.id.action_homeFragment_to_expensesFragment)
        }

        // TODO: Set up Rewards section logic
        binding.rewardsLabelTextView.setOnClickListener { // Temp navigation trigger
            findNavController().navigate(R.id.action_homeFragment_to_rewardsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 