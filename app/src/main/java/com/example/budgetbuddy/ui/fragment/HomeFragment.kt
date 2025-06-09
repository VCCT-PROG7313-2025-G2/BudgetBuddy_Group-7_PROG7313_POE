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
import com.example.budgetbuddy.ui.viewmodel.FirebaseHomeUiState
import com.example.budgetbuddy.ui.viewmodel.FirebaseHomeViewModel
import com.example.budgetbuddy.util.CurrencyConverter
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
import com.example.budgetbuddy.util.toIntSafe
import javax.inject.Inject
import android.util.Log

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FirebaseHomeViewModel by viewModels()
    private lateinit var categoryAdapter: HomeCategoryAdapter

    // Inject CurrencyConverter for proper currency formatting
    @Inject
    lateinit var currencyConverter: CurrencyConverter

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
        Log.d("HomeFragment", "Setting up button clicks and navigation")
        
        // When they tap the "Add Expense" button, take them to the expense entry screen
        binding.addExpenseButton.setOnClickListener {
            Log.d("HomeFragment", "User wants to add a new expense")
            findNavController().navigate(R.id.action_homeFragment_to_newExpenseFragment)
        }
        
        // Settings button for app configuration
        binding.settingsButton.setOnClickListener {
            Log.d("HomeFragment", "User opened settings")
            findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
        }

        // These cards both lead to budget setup - makes sense since they show budget info
        val budgetSetupAction = R.id.action_homeFragment_to_budgetSetupFragment
        binding.balanceCardView.setOnClickListener {
            Log.d("HomeFragment", "User tapped balance card - taking them to budget setup")
            findNavController().navigate(budgetSetupAction)
        }
        binding.budgetCategoriesCardView.setOnClickListener {
            Log.d("HomeFragment", "User tapped categories card - taking them to budget setup")
            findNavController().navigate(budgetSetupAction)
        }

        // For rewards, we need to use the bottom navigation to maintain proper state
        binding.rewardsContainer.setOnClickListener {
            Log.d("HomeFragment", "User wants to see rewards - switching to rewards tab")
            // Find the bottom navigation and simulate a tap on the rewards tab
            val bottomNavigation = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigation?.selectedItemId = R.id.rewardsFragment
        }
    }

    // Observes data changes from the HomeViewModel and updates the UI.
    private fun observeViewModel() {
        // Use lifecycleScope to automatically manage observation based on fragment lifecycle.
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle ensures collection stops when the fragment is stopped and restarts when started.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect the latest UI state from the ViewModel's flow.
                viewModel.uiState.collect { state ->
                    Log.d("HomeFragment", "Got new data from ViewModel, updating the UI...")
                    
                    // Update the greeting text.
                    binding.greetingTextView.text = state.greeting
                    
                    // Format and display budget numbers using CurrencyConverter
                    val spentAmount = currencyConverter.formatAmount(state.budgetSpent)
                    val totalAmount = currencyConverter.formatAmount(state.budgetTotal)
                    binding.balanceAmountTextView.text = "$spentAmount / $totalAmount"
                    
                    // Update the progress bar to show how much budget has been used
                    binding.budgetProgressBar.max = state.budgetTotal.toIntSafe() // Use safe conversion
                    binding.budgetProgressBar.progress = state.budgetSpent.toIntSafe()
                    
                    Log.d("HomeFragment", "Budget display: $spentAmount / $totalAmount")
                    
                    // Show/hide rewards section based on data.
                    binding.rewardsContainer.isVisible = state.leaderboardPositionText.isNotEmpty()
                    binding.rewardsTextView.text = state.leaderboardPositionText

                    // Update the list of budget categories.
                    categoryAdapter.updateData(state.budgetCategories)
                    Log.d("HomeFragment", "Updated category list with ${state.budgetCategories.size} categories")

                    // Update the bar chart if data is available, otherwise clear it.
                    state.dailySpendingData?.let { chartData ->
                        Log.d("HomeFragment", "Updating spending chart with fresh data")
                        updateSpendingTrendChart(chartData.first, chartData.second)
                    } ?: run {
                        Log.d("HomeFragment", "No chart data available, clearing the chart")
                        binding.spendingTrendChartView.clear()
                        binding.spendingTrendChartView.invalidate()
                    }

                    // Show errors using a Toast message.
                    state.error?.let { errorMessage ->
                        Log.e("HomeFragment", "Showing error to user: $errorMessage")
                        Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Updates the bar chart with new data.
    private fun updateSpendingTrendChart(entries: List<BarEntry>, labels: List<String>) {
        val chart: BarChart = binding.spendingTrendChartView
        Log.d("HomeFragment", "Setting up spending chart with ${entries.size} data points")
        
        // If no data, clear the chart.
        if (entries.isEmpty()) {
            Log.d("HomeFragment", "No spending data to show, clearing chart")
            chart.clear()
            chart.invalidate()
            return
        }

        // Create a dataset for the bars - this is where the magic happens
        val dataSet = BarDataSet(entries, "Daily Spending")
        dataSet.color = android.graphics.Color.BLACK // Keep it simple with black bars
        dataSet.setDrawValues(false) // Don't clutter the chart with numbers on top

        // Set the data for the chart.
        val barData = BarData(dataSet)
        chart.data = barData

        Log.d("HomeFragment", "Chart configured with ${labels.joinToString(", ")} as day labels")

        // --- Make the chart look good ---
        chart.description.isEnabled = false // We don't need a description
        chart.legend.isEnabled = false // Keep it clean, no legend needed
        chart.setTouchEnabled(false) // This is just for viewing, not interacting
        chart.setDrawGridBackground(false) // Clean look without background grid
        chart.setDrawBarShadow(false) // No fancy shadows needed

        // Set up the bottom labels (days of the week)
        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels) // Use our day names
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false) // Clean look
        xAxis.granularity = 1f // One label per day
        xAxis.labelCount = labels.size // Show all the day labels

        // Set up the left side labels (spending amounts)
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(false) // Keep it clean
        leftAxis.setDrawAxisLine(true) // But show the main axis line
        leftAxis.setDrawLabels(true) // Show the amount labels
        leftAxis.axisMinimum = 0f // Always start from zero

        // Hide the right side axis - we don't need it
        chart.axisRight.isEnabled = false
        
        // Refresh the chart to show all our changes
        chart.invalidate()
        Log.d("HomeFragment", "Chart updated and ready to display!")
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