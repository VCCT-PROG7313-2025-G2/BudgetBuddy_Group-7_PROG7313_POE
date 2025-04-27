package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Placeholder
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.concurrent.TimeUnit

// Imports for RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetbuddy.databinding.ItemHomeCategoryBinding // Import item binding

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // --- Adapter and Data Class (Inner classes for simplicity) ---

    // 1. Data class for category item
    data class HomeCategoryItem(
        val iconResId: Int, // Placeholder resource ID
        val name: String,
        val progress: Int, // 0-100
        val percentageText: String
    )

    // 2. RecyclerView Adapter
    class HomeCategoryAdapter(private val categories: List<HomeCategoryItem>) :
        RecyclerView.Adapter<HomeCategoryAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemHomeCategoryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemHomeCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]
            holder.binding.categoryIconImageView.setImageResource(category.iconResId)
            holder.binding.categoryNameTextView.text = category.name
            holder.binding.categoryProgressBar.progress = category.progress
            holder.binding.categoryPercentageTextView.text = category.percentageText
            // TODO: Set progress bar max based on actual budget limit if needed
        }

        override fun getItemCount() = categories.size
    }

    // --- End Adapter and Data Class ---

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

        // TODO: Populate UI with actual data (greeting, balance, charts, lists)
        binding.greetingTextView.text = "Hi, Alex" // Placeholder
        binding.balanceAmountTextView.text = "$3,450 / $5,000" // Placeholder

        binding.addExpenseButton.setOnClickListener {
            // Navigate to New Expense screen (Screen 5) using the defined action
            findNavController().navigate(R.id.action_homeFragment_to_newExpenseFragment)
            // Toast.makeText(context, "Add Expense Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        binding.notificationsButton.setOnClickListener {
             // TODO: Handle notifications click
            Toast.makeText(context, "Notifications Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }
        
        // TODO: Handle help button click
        binding.helpButton.setOnClickListener {
            Toast.makeText(context, "Help Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
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

        setupSpendingTrendChart()
        setupBudgetCategoriesRecyclerView()
    }

    private fun setupBudgetCategoriesRecyclerView() {
        // 3. Create placeholder data
        // TODO: Replace with actual budget category data from ViewModel/Repository
        val placeholderCategories = listOf(
            HomeCategoryItem(R.drawable.ic_category_utilities, "Housing", 75, "75%"), // Use utility icon for housing
            HomeCategoryItem(R.drawable.ic_category_food, "Food", 60, "60%"),
            HomeCategoryItem(R.drawable.ic_category_transport, "Transport", 45, "45%"),
            HomeCategoryItem(R.drawable.ic_category_shopping, "Entertainment", 85, "85%") // Use shopping icon for entertainment
        )

        // 4. Create adapter instance
        val categoryAdapter = HomeCategoryAdapter(placeholderCategories)

        // 5. Setup RecyclerView
        binding.budgetCategoriesRecyclerView.apply {
            // LayoutManager is already set in XML, but setting here is also fine
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
    }

    private fun setupSpendingTrendChart() {
        val chart: BarChart = binding.spendingTrendChartView

        // --- Placeholder Data --- 
        // TODO: Replace this with actual data fetching and processing logic
        // You'll need to query your expense data, group by day, and sum amounts.
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        // Example: Last 7 days (adjust as needed)
        val daysToShow = 7
        val todayMillis = System.currentTimeMillis()
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (i in (daysToShow - 1) downTo 0) {
            val dayMillis = todayMillis - TimeUnit.DAYS.toMillis(i.toLong())
            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = dayMillis }
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, ... 7=Sat

            // Placeholder spending amount (e.g., random) - Replace with actual calculation
            val spending = (20..100).random().toFloat()

            entries.add(BarEntry((daysToShow - 1 - i).toFloat(), spending))
            labels.add(dayLabels[dayOfWeek - 1]) 
        }
        // --- End Placeholder Data ---

        val dataSet = BarDataSet(entries, "Daily Spending")
        dataSet.color = android.graphics.Color.BLACK // Set bars to black
        dataSet.setDrawValues(false) // Hide values on top of bars

        val barData = BarData(dataSet)

        // --- Chart Configuration --- 
        chart.data = barData
        chart.description.isEnabled = false // No description text
        chart.legend.isEnabled = false // No legend
        chart.setTouchEnabled(false) // Disable touch interactions (optional)
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        
        // X-Axis Configuration
        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels) // Set day labels
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f // Ensure all labels are shown
        xAxis.labelCount = labels.size

        // Y-Axis Configuration (Left)
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawAxisLine(false)
        leftAxis.setDrawLabels(true)
        leftAxis.axisMinimum = 0f // Start at 0
        leftAxis.axisMaximum = 5000f // Set maximum to 5000

        // Y-Axis Configuration (Right)
        chart.axisRight.isEnabled = false // Disable right axis

        chart.invalidate() // Refresh the chart
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 