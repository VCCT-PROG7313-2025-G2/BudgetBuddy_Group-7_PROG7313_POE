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

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var categoryAdapter: HomeCategoryAdapter

    // --- RecyclerView Adapter (Moved outside data class for clarity) ---
    class HomeCategoryAdapter(private var categories: List<HomeCategoryItemUiState>) :
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
        }

        override fun getItemCount() = categories.size

        // Function to update the adapter's data
        fun updateData(newCategories: List<HomeCategoryItemUiState>) {
            categories = newCategories
            notifyDataSetChanged() // TODO: Use DiffUtil for better performance
        }
    }
    // --- End RecyclerView Adapter ---

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

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        categoryAdapter = HomeCategoryAdapter(emptyList()) // Initialize with empty list
        binding.budgetCategoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
            // Set fixed size false if height is wrap_content, true if fixed height (like 200dp)
            setHasFixedSize(true) // Optimization since item size doesn't change
        }
    }

    private fun setupClickListeners() {
        binding.addExpenseButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_newExpenseFragment)
        }
        binding.notificationsButton.setOnClickListener {
            Toast.makeText(context, "Notifications Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }
        binding.helpButton.setOnClickListener {
            Toast.makeText(context, "Help Clicked (Not implemented)", Toast.LENGTH_SHORT).show()
        }

        // Navigate to Budget Setup when edit icons are clicked
        val budgetSetupAction = R.id.action_homeFragment_to_budgetSetupFragment
        binding.editBudgetButton.setOnClickListener {
            findNavController().navigate(budgetSetupAction)
        }
        binding.editCategoriesButton.setOnClickListener {
            findNavController().navigate(budgetSetupAction)
        }

        // Remove temp navigation triggers
        // binding.budgetCategoriesLabelTextView.setOnClickListener { ... }
        // binding.spendingTrendChartView.setOnClickListener { ... }
        // binding.rewardsLabelTextView.setOnClickListener { ... }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update UI based on the state
                    binding.greetingTextView.text = state.greeting
                    // TODO: Format currency correctly based on locale/settings
                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US) // Example
                    binding.balanceAmountTextView.text = "${currencyFormat.format(state.budgetSpent)} / ${currencyFormat.format(state.budgetTotal)}"
                    binding.budgetProgressBar.max = state.budgetTotal.toInt() // Assuming total budget fits in Int for progress bar max
                    binding.budgetProgressBar.progress = state.budgetSpent.toInt()
                    binding.rewardsContainer.isVisible = state.rewardsText.isNotEmpty() // Show rewards card only if there's text
                    binding.rewardsTextView.text = state.rewardsText

                    // Update Category RecyclerView
                    categoryAdapter.updateData(state.budgetCategories)

                    // Update Bar Chart
                    state.dailySpendingData?.let {
                        updateSpendingTrendChart(it.first, it.second)
                    } ?: run {
                        // Handle case where chart data is not available (e.g., show empty state)
                        binding.spendingTrendChartView.clear()
                    }

                    // Show loading indicator (optional)
                    // binding.loadingIndicator.isVisible = state.isLoading

                    // Show error message (optional)
                    state.error?.let {
                        Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
                        // TODO: Clear error from state after showing?
                    }
                }
            }
        }
    }

    private fun updateSpendingTrendChart(entries: List<BarEntry>, labels: List<String>) {
        val chart: BarChart = binding.spendingTrendChartView
        if (entries.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        val dataSet = BarDataSet(entries, "Daily Spending")
        dataSet.color = android.graphics.Color.BLACK
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        chart.data = barData

        // Configure Axis (Consider moving static config to onViewCreated if preferred)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)

        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelCount = labels.size

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawAxisLine(false)
        leftAxis.setDrawLabels(false)
        leftAxis.axisMinimum = 0f

        chart.axisRight.isEnabled = false
        chart.invalidate() // Refresh chart
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