package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Placeholder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentHomeBinding
import com.example.budgetbuddy.databinding.ItemHomeCategoryBinding // Import item binding
import com.example.budgetbuddy.ui.viewmodel.HomeCategoryItemUiState
import com.example.budgetbuddy.ui.viewmodel.HomeUiState
import com.example.budgetbuddy.ui.viewmodel.HomeViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.text.NumberFormat // For currency formatting
import java.util.Locale
import java.math.BigDecimal

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var categoryAdapter: HomeCategoryAdapter

    // --- RecyclerView Adapter (Manages how category data is shown in the list) ---
    class HomeCategoryAdapter(private var categories: List<HomeCategoryItemUiState>) :
        RecyclerView.Adapter<HomeCategoryAdapter.ViewHolder>() {

        // Holds references to the views for a single category item in the list.
        class ViewHolder(val binding: ItemHomeCategoryBinding) : RecyclerView.ViewHolder(binding.root)

        // Creates a new ViewHolder when the RecyclerView needs one.
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Inflate the layout for a single category item.
            val binding = ItemHomeCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        // Populates the views in a ViewHolder with data for a specific category.
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]
            holder.binding.categoryIconImageView.setImageResource(category.iconResId)
            holder.binding.categoryNameTextView.text = category.name
            holder.binding.categoryProgressBar.progress = category.progress
            holder.binding.categoryPercentageTextView.text = category.percentageText
        }

        // Returns the total number of categories in the list.
        override fun getItemCount() = categories.size

        // Updates the list of categories the adapter is displaying.
        fun updateData(newCategories: List<HomeCategoryItemUiState>) {
            categories = newCategories
            notifyDataSetChanged() // Tells the RecyclerView to redraw itself.
        }
    }
    // --- End RecyclerView Adapter ---

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate (create) the layout for this fragment using View Binding.
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the list (RecyclerView) for budget categories.
        setupRecyclerView()
        // Set up button clicks and other interactions.
        setupClickListeners()
        // Start listening for data updates from the ViewModel.
        observeViewModel()
    }

    // Configures the RecyclerView and its adapter.
    private fun setupRecyclerView() {
        categoryAdapter = HomeCategoryAdapter(emptyList()) // Start with no categories.
        binding.budgetCategoriesRecyclerView.apply {
            // Arrange items in a vertical list.
            layoutManager = LinearLayoutManager(context)
            // Set the adapter created earlier.
            adapter = categoryAdapter
            // Optimize if item heights are fixed.
            setHasFixedSize(true)
        }
    }

    // Sets up what happens when buttons or cards are clicked.
    private fun setupClickListeners() {
        // Go to the "Add New Expense" screen when the button is clicked.
        binding.addExpenseButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_newExpenseFragment)
        }
        // Go to the Settings screen.
        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }

        // Go to the Budget Setup screen from various places.
        val budgetSetupAction = R.id.action_homeFragment_to_budgetSetupFragment
        binding.balanceCardView.setOnClickListener {
            findNavController().navigate(budgetSetupAction)
        }
        binding.budgetCategoriesCardView.setOnClickListener {
            findNavController().navigate(budgetSetupAction)
        }

        // Go to the Rewards screen.
        binding.rewardsContainer.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_rewardsFragment)
        }

        // Old temporary navigation clicks were removed.
        // binding.budgetCategoriesLabelTextView.setOnClickListener { ... }
        // binding.spendingTrendChartView.setOnClickListener { ... }
        // binding.rewardsLabelTextView.setOnClickListener { ... }
    }

    // Observes data changes from the HomeViewModel and updates the UI.
    private fun observeViewModel() {
        // Use lifecycleScope to automatically manage observation based on fragment lifecycle.
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle ensures collection stops when the fragment is stopped and restarts when started.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect the latest UI state from the ViewModel's flow.
                viewModel.uiState.collect { state ->
                    // Update the greeting text.
                    binding.greetingTextView.text = state.greeting
                    // Format and display budget numbers.
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US) // Use appropriate locale
                    binding.balanceAmountTextView.text = "${currencyFormat.format(state.budgetSpent)} / ${currencyFormat.format(state.budgetTotal)}"
                    binding.budgetProgressBar.max = state.budgetTotal.toIntSafe() // Use safe conversion
                    binding.budgetProgressBar.progress = state.budgetSpent.toIntSafe()
                    // Show/hide rewards section based on data.
                    binding.rewardsContainer.isVisible = state.leaderboardPositionText.isNotEmpty()
                    binding.rewardsTextView.text = state.leaderboardPositionText

                    // Update the list of budget categories.
                    categoryAdapter.updateData(state.budgetCategories)

                    // Update the bar chart if data is available, otherwise clear it.
                    state.dailySpendingData?.let {
                        updateSpendingTrendChart(it.first, it.second)
                    } ?: run {
                        binding.spendingTrendChartView.clear()
                        binding.spendingTrendChartView.invalidate()
                    }

                    // Show loading indicator (optional, depends on state definition).
                    // binding.loadingIndicator.isVisible = state.isLoading

                    // Show errors using a Toast message.
                    state.error?.let {
                        Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
                        // viewModel.clearError() // Maybe add a function to clear the error after showing
                    }
                }
            }
        }
    }

    // Updates the bar chart with new data.
    private fun updateSpendingTrendChart(entries: List<BarEntry>, labels: List<String>) {
        val chart: BarChart = binding.spendingTrendChartView
        // If no data, clear the chart.
        if (entries.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        // Create a dataset for the bars.
        val dataSet = BarDataSet(entries, "Daily Spending")
        dataSet.color = android.graphics.Color.BLACK // Bar color
        dataSet.setDrawValues(false) // Don't show numbers on top of bars

        // Set the data for the chart.
        val barData = BarData(dataSet)
        chart.data = barData

        // --- Chart Appearance Configuration ---
        chart.description.isEnabled = false // Hide chart description
        chart.legend.isEnabled = false // Hide legend
        chart.setTouchEnabled(false) // Disable touch interactions
        chart.setDrawGridBackground(false) // No background grid
        chart.setDrawBarShadow(false) // No shadows behind bars

        // X-Axis (Bottom labels - days of the week)
        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels) // Use provided labels (e.g., "Mon", "Tue")
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false) // No vertical grid lines
        xAxis.granularity = 1f // Minimum interval between labels
        xAxis.labelCount = labels.size // Try to show all labels

        // Y-Axis (Left labels - spending amount)
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(false) // No horizontal grid lines
        leftAxis.setDrawAxisLine(true) // Show the axis line itself
        leftAxis.setDrawLabels(true) // Show amount labels
        leftAxis.axisMinimum = 0f // Start Y-axis at zero

        // Hide the right Y-axis.
        chart.axisRight.isEnabled = false
        // Refresh the chart to show changes.
        chart.invalidate()
    }

    // Helper function to safely convert BigDecimal to Int for ProgressBar
    // Returns 0 if conversion would overflow or is negative
    private fun BigDecimal.toIntSafe(): Int {
        return this.max(BigDecimal.ZERO)
                   .min(BigDecimal(Int.MAX_VALUE.toDouble()))
                   .toInt()
    }

    // Hide the main toolbar when this fragment is shown.
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    // Show the main toolbar again when leaving this fragment.
    override fun onPause() {
        super.onPause()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    // Clean up the binding when the view is destroyed to prevent memory leaks.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 