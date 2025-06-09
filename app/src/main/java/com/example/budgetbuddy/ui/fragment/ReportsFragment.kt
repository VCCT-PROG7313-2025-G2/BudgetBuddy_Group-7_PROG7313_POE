package com.example.budgetbuddy.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentReportsBinding
import com.example.budgetbuddy.ui.viewmodel.FirebaseReportsViewModel
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import java.text.NumberFormat
import com.example.budgetbuddy.util.CurrencyConverter
import javax.inject.Inject

/**
 * Reports Fragment - Shows spending insights and analytics
 */
@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FirebaseReportsViewModel by viewModels()

    // Inject CurrencyConverter for proper currency formatting
    @Inject
    lateinit var currencyConverter: CurrencyConverter

    private val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    // Track whether to show amounts or percentages
    private var showAmounts = true

    // Time period analysis variables
    private val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private var selectedTimePeriod = FirebaseReportsViewModel.TimePeriod.MONTH
    private var selectedCategory: String? = null // For category filtering
    private var customStartDate: Date? = null
    private var customEndDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        android.util.Log.d("ReportsFragment", "=== onCreateView called ===")
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("ReportsFragment", "=== onViewCreated called ===")
        
        setupUI()
        setupClickListeners()
        observeViewModel()
        loadCurrentMonthData()
    }

    private fun setupUI() {
        // Set current month/year
        updateMonthYearDisplay()
        
        // Setup pie chart for category spending
        setupPieChart()
        
        // Setup bar chart for daily spending
        setupBarChart()
        
        // Setup time period analysis
        setupTimePeriodAnalysis()
        
        // Setup download button
        binding.downloadReportButton.text = "Export Report"
        
        // Initialize toggle button
        binding.categoryDisplayToggleButton.text = if (showAmounts) "Amount" else "Percentage"
    }

    private fun setupClickListeners() {
        binding.previousMonthNavButton.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateMonthYearDisplay()
            loadMonthData()
        }

        binding.nextMonthNavButton.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateMonthYearDisplay()
            loadMonthData()
        }

        binding.downloadReportButton.setOnClickListener {
            exportReport()
        }

        binding.categoryDisplayToggleButton.setOnClickListener {
            toggleCategoryDisplay()
        }

        // Time period analysis click listeners
        setupTimePeriodClickListeners()
    }

    private fun setupTimePeriodAnalysis() {
        // Setup line chart for spending over time
        with(binding.spendingOverTimeLineChart) {
            description = Description().apply { text = "" }
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            // Configure X-axis
            xAxis.apply {
                isEnabled = true
                setDrawGridLines(true)
                setDrawAxisLine(true)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                textColor = Color.BLACK
                textSize = 10f
                granularity = 1f
                setLabelCount(7, false)
            }
            
            // Configure left Y-axis
            axisLeft.apply {
                isEnabled = true
                setDrawGridLines(true)
                setDrawAxisLine(true)
                textColor = Color.BLACK
                textSize = 10f
                axisMinimum = 0f
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                        return currencyConverter.formatAmount(value.toDouble())
                    }
                }
            }
            
            // Hide right Y-axis
            axisRight.isEnabled = false
            
            // Configure legend
            legend.isEnabled = false
            
            // Set margins
            setExtraOffsets(10f, 10f, 10f, 10f)
        }

        // Setup category chips
        setupCategoryChips()

        // Load default period (Last 30 Days)
        loadTimePeriodAnalysisWithFilters()
    }

    private fun setupTimePeriodClickListeners() {
        // Time period chip selection
        binding.timePeriodChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chipWeek -> {
                        selectedTimePeriod = FirebaseReportsViewModel.TimePeriod.WEEK
                        binding.customDateRangeLayout.visibility = View.GONE
                        loadTimePeriodAnalysisWithFilters()
                    }
                    R.id.chipMonth -> {
                        selectedTimePeriod = FirebaseReportsViewModel.TimePeriod.MONTH
                        binding.customDateRangeLayout.visibility = View.GONE
                        loadTimePeriodAnalysisWithFilters()
                    }
                    R.id.chipQuarter -> {
                        selectedTimePeriod = FirebaseReportsViewModel.TimePeriod.QUARTER
                        binding.customDateRangeLayout.visibility = View.GONE
                        loadTimePeriodAnalysisWithFilters()
                    }
                    R.id.chipYear -> {
                        selectedTimePeriod = FirebaseReportsViewModel.TimePeriod.YEAR
                        binding.customDateRangeLayout.visibility = View.GONE
                        loadTimePeriodAnalysisWithFilters()
                    }
                }
            }
        }

        // Custom date range selection
        binding.startDateEditText.setOnClickListener {
            showDatePicker { date ->
                customStartDate = date
                binding.startDateEditText.setText(dateFormatter.format(date))
                updateCustomDateRange()
            }
        }

        binding.endDateEditText.setOnClickListener {
            showDatePicker { date ->
                customEndDate = date
                binding.endDateEditText.setText(dateFormatter.format(date))
                updateCustomDateRange()
            }
        }
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateCustomDateRange() {
        if (customStartDate != null && customEndDate != null) {
            if (customStartDate!!.before(customEndDate) || customStartDate!!.equals(customEndDate)) {
                selectedTimePeriod = FirebaseReportsViewModel.TimePeriod.CUSTOM
                loadTimePeriodAnalysisWithFilters()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe monthly summary
                launch {
                    viewModel.monthlySummary.collect { summary ->
                        android.util.Log.d("ReportsFragment", "=== Monthly Summary Received ===")
                        android.util.Log.d("ReportsFragment", "Total spent: ${summary.totalSpent}")
                        android.util.Log.d("ReportsFragment", "Budget amount: ${summary.budgetAmount}")
                        android.util.Log.d("ReportsFragment", "Percentage change: ${summary.percentageChange}")
                        updateMonthlySummary(summary)
                    }
                }

                // Observe category spending
                launch {
                    viewModel.categorySpending.collect { categoryData ->
                        android.util.Log.d("ReportsFragment", "=== Category Spending Received ===")
                        android.util.Log.d("ReportsFragment", "Number of categories: ${categoryData.size}")
                        categoryData.forEach { category ->
                            android.util.Log.d("ReportsFragment", "Category: ${category.categoryName}, Amount: ${category.amount}, Percentage: ${category.percentage}%")
                        }
                        updateCategoryChart(categoryData)
                        updateCategoryLegend(categoryData)
                    }
                }

                // Observe spending trend
                launch {
                    viewModel.spendingTrend.collect { trendData ->
                        android.util.Log.d("ReportsFragment", "=== Spending Trend Received ===")
                        android.util.Log.d("ReportsFragment", "Is increasing: ${trendData.isIncreasing}")
                        android.util.Log.d("ReportsFragment", "Is decreasing: ${trendData.isDecreasing}")
                        android.util.Log.d("ReportsFragment", "Change amount: ${trendData.changeAmount}")
                        updateSpendingTrend(trendData)
                    }
                }

                // Observe weekly spending
                launch {
                    viewModel.weeklySpending.collect { weeklyData ->
                        android.util.Log.d("ReportsFragment", "=== Weekly Spending Received ===")
                        android.util.Log.d("ReportsFragment", "Number of weeks: ${weeklyData.size}")
                        updateWeeklySpendingChart(weeklyData)
                    }
                }

                // Observe time period analysis
                launch {
                    viewModel.timePeriodAnalysis.collect { analysis ->
                        android.util.Log.d("ReportsFragment", "=== Time Period Analysis Received ===")
                        android.util.Log.d("ReportsFragment", "Total: ${analysis.totalSpent}, Daily Avg: ${analysis.dailyAverage}, Trend: ${analysis.trend}")
                        updateTimePeriodAnalysisSummary(analysis)
                    }
                }

                // Observe period spending data
                launch {
                    viewModel.periodSpendingData.collect { spendingData ->
                        android.util.Log.d("ReportsFragment", "=== Period Spending Data Received ===")
                        android.util.Log.d("ReportsFragment", "Number of data points: ${spendingData.size}")
                        updateSpendingOverTimeChart(spendingData)
                    }
                }
            }
        }
    }

    private fun updateMonthYearDisplay() {
        binding.monthYearTextView.text = monthFormatter.format(calendar.time)
    }

    private fun loadCurrentMonthData() {
        android.util.Log.d("ReportsFragment", "=== Loading current month data ===")
        android.util.Log.d("ReportsFragment", "Current calendar: ${calendar.time}")
        loadMonthData()
    }

    private fun loadMonthData() {
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        android.util.Log.d("ReportsFragment", "=== Loading reports data for month: $monthYear ===")
        android.util.Log.d("ReportsFragment", "Calendar date: ${calendar.time}")
        
        viewModel.loadReportsData(monthYear)
    }

    private fun setupPieChart() {
        with(binding.categoryPieChart) {
            // Configure pie chart for spending visualization
            description = Description().apply { text = "" }
            setUsePercentValues(false)
            setDrawHoleEnabled(true)
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 35f
            transparentCircleRadius = 40f
            setDrawCenterText(false) // Remove center text
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            
            // Remove entry labels (text on slices)
            setDrawEntryLabels(false)
            
            // Remove default legend (we'll use custom)
            legend.isEnabled = false
            
            // Add some padding for better visibility
            setExtraOffsets(20f, 20f, 20f, 20f)
        }
    }

    private fun setupBarChart() {
        with(binding.dailySpendingBarChart) {
            // Configure bar chart for daily spending visualization
            description = Description().apply { text = "" }
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(false)
            
            // Configure X-axis
            xAxis.apply {
                isEnabled = true
                setDrawGridLines(false)
                setDrawAxisLine(true)
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                textColor = Color.BLACK
                textSize = 12f
                granularity = 1f
                setDrawLabels(true)
                axisMinimum = 0f
                axisMaximum = 5f
                setLabelCount(5, false)
                setCenterAxisLabels(false)
                setAvoidFirstLastClipping(false)
                // Custom formatter to show week numbers only
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                        return when (value.toInt()) {
                            1 -> "Week 1"
                            2 -> "Week 2" 
                            3 -> "Week 3"
                            4 -> "Week 4"
                            else -> ""
                        }
                    }
                }
            }
            
            // Configure left Y-axis
            axisLeft.apply {
                isEnabled = true
                setDrawGridLines(true)
                setDrawAxisLine(true)
                textColor = Color.BLACK
                textSize = 10f
                axisMinimum = 0f
                granularity = 50f // Set granularity for Y-axis
            }
            
            // Disable right Y-axis
            axisRight.isEnabled = false
            
            // Remove legend
            legend.isEnabled = false
            
            // Enable touch interactions
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            
            // Add padding
            setExtraOffsets(10f, 10f, 10f, 10f)
        }
    }

    private fun updateMonthlySummary(summary: FirebaseReportsViewModel.MonthlySummary) {
        // Display total spending prominently
        binding.totalSpendingAmountTextView.text = currencyConverter.formatAmount(summary.totalSpent)
        
        // Create detailed spending change text with better formatting
        val changeText = when {
            summary.percentageChange > 0 -> "↑ ${String.format("%.1f", summary.percentageChange)}% increase from last month"
            summary.percentageChange < 0 -> "↓ ${String.format("%.1f", Math.abs(summary.percentageChange))}% decrease from last month"
            else -> "→ Same as last month"
        }
        
        binding.spendingChangeTextView.text = changeText
        
        // Color code the change indicator
        val changeColor = when {
            summary.percentageChange > 0 -> Color.parseColor("#FF5722") // Red for increase
            summary.percentageChange < 0 -> Color.parseColor("#4CAF50") // Green for decrease
            else -> Color.parseColor("#757575") // Gray for no change
        }
        binding.spendingChangeTextView.setTextColor(changeColor)
        
        // Update the main label to be more descriptive
        binding.totalSpendingLabel.text = "Total Monthly Spending"
        
        // Log for debugging
        android.util.Log.d("ReportsFragment", "Updated summary: Total: R${summary.totalSpent}, Change: ${summary.percentageChange}%")
    }

    private fun updateCategoryChart(categoryData: List<FirebaseReportsViewModel.CategorySpending>) {
        android.util.Log.d("ReportsFragment", "=== Updating Category Chart ===")
        android.util.Log.d("ReportsFragment", "Number of categories to display: ${categoryData.size}")
        
        if (categoryData.isEmpty()) {
            android.util.Log.d("ReportsFragment", "No category data - hiding chart")
            binding.categoryPieChart.clear()
            binding.categoryPieChart.invalidate()
            binding.categoryPieChart.setCenterText("No Spending\nData")
            return
        }
        
        android.util.Log.d("ReportsFragment", "Category data is not empty - showing chart")
        binding.categoryPieChart.clear()
        binding.categoryPieChart.invalidate()

        val entries = categoryData.map { category ->
            android.util.Log.d("ReportsFragment", "Adding pie entry: ${category.categoryName} = ${category.amount}")
            PieEntry(category.amount.toFloat(), category.categoryName)
        }

        val dataSet = PieDataSet(entries, "Spending by Category").apply {
            // Use grayscale colors only (shades of black/gray)
            val chartColors = listOf(
                Color.parseColor("#212121"), // Very Dark Gray (almost black)
                Color.parseColor("#424242"), // Dark Gray
                Color.parseColor("#616161"), // Medium Dark Gray
                Color.parseColor("#757575"), // Medium Gray
                Color.parseColor("#9E9E9E"), // Light Gray
                Color.parseColor("#BDBDBD"), // Lighter Gray
                Color.parseColor("#E0E0E0"), // Very Light Gray
                Color.parseColor("#F5F5F5")  // Almost White Gray
            )
            colors = chartColors
            
            // Remove value text (no text on slices)
            setDrawValues(false)
            
            // Enhance slice appearance
            sliceSpace = 3f
            selectionShift = 10f
        }

        val data = PieData(dataSet)
        binding.categoryPieChart.data = data
        
        // Update center text with total
        val totalSpent = categoryData.sumOf { it.amount }
        binding.categoryPieChart.setCenterText("Total\n${currencyConverter.formatAmount(totalSpent)}")
        
        binding.categoryPieChart.invalidate()
        
        android.util.Log.d("ReportsFragment", "Updated pie chart with ${categoryData.size} categories, total: R${totalSpent}")
    }

    private fun updateCategoryLegend(categoryData: List<FirebaseReportsViewModel.CategorySpending>) {
        binding.categoryLegendLayout.removeAllViews()
        
        if (categoryData.isEmpty()) {
            // Show message when no spending data
            val noDataView = android.widget.TextView(requireContext()).apply {
                text = "No spending data for this month"
                setPadding(16, 16, 16, 16)
                textSize = 16f
                setTextColor(Color.parseColor("#757575"))
                gravity = android.view.Gravity.CENTER
            }
            binding.categoryLegendLayout.addView(noDataView)
            return
        }
        
        // Add header for category breakdown
        val headerView = android.widget.TextView(requireContext()).apply {
            text = "Spending by Category"
            setPadding(16, 16, 16, 8)
            textSize = 18f
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        binding.categoryLegendLayout.addView(headerView)
        
        // Add each category with enhanced formatting
        categoryData.forEachIndexed { index, category ->
            val categoryContainer = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(16, 12, 16, 12)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            
            // Color indicator (circle)
            val colorIndicator = android.widget.TextView(requireContext()).apply {
                text = "●"
                textSize = 20f
                setPadding(0, 0, 16, 0)
                // Use grayscale colors to match chart
                val colors = arrayOf("#212121", "#424242", "#616161", "#757575", "#9E9E9E", "#BDBDBD", "#E0E0E0", "#F5F5F5")
                setTextColor(Color.parseColor(colors[index % colors.size]))
            }
            
            // Category info container
            val infoContainer = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            // Category name
            val categoryNameView = android.widget.TextView(requireContext()).apply {
                text = category.categoryName
                textSize = 16f
                setTextColor(Color.BLACK)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            
            // Amount or percentage based on current display mode
            val amountView = android.widget.TextView(requireContext()).apply {
                text = if (showAmounts) {
                    currencyConverter.formatAmount(category.amount)
                } else {
                    "${String.format("%.1f", category.percentage)}%"
                }
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
            }
            
            // Assemble the views
            infoContainer.addView(categoryNameView)
            infoContainer.addView(amountView)
            categoryContainer.addView(colorIndicator)
            categoryContainer.addView(infoContainer)
            
            binding.categoryLegendLayout.addView(categoryContainer)
            
            // Add separator line (except for last item)
            if (index < categoryData.size - 1) {
                val separator = View(requireContext()).apply {
                    val params = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                        1
                    )
                    params.setMargins(32, 8, 32, 8)
                    layoutParams = params
                    setBackgroundColor(Color.parseColor("#E0E0E0"))
                }
                binding.categoryLegendLayout.addView(separator)
            }
        }
        
        android.util.Log.d("ReportsFragment", "Updated category legend with ${categoryData.size} categories")
    }

    private fun updateSpendingTrend(trendData: FirebaseReportsViewModel.SpendingTrend) {
        binding.spendingChangeTextView.text = when {
            trendData.isIncreasing -> "↑ Spending increased this month"
            trendData.isDecreasing -> "↓ Spending decreased this month"
            else -> "→ Spending remained stable"
        }
    }

    private fun updateWeeklySpendingChart(weeklyData: List<FirebaseReportsViewModel.WeeklySpending>) {
        android.util.Log.d("ReportsFragment", "=== Updating Weekly Spending Chart ===")
        android.util.Log.d("ReportsFragment", "Number of weeks to display: ${weeklyData.size}")
        
        if (weeklyData.isEmpty()) {
            android.util.Log.d("ReportsFragment", "No weekly data - clearing chart")
            binding.dailySpendingBarChart.clear()
            binding.dailySpendingBarChart.invalidate()
            return
        }
        
        // Create bar entries for all weeks (show even weeks with 0 spending)
        val entries = weeklyData.map { weekData ->
            android.util.Log.d("ReportsFragment", "Adding bar entry: Week ${weekData.week} = R${weekData.amount}")
            BarEntry(weekData.week.toFloat(), weekData.amount.toFloat())
        }
        
        val dataSet = BarDataSet(entries, "Weekly Spending").apply {
            // Use grayscale colors for consistency
            color = Color.parseColor("#424242") // Dark gray
            valueTextColor = Color.BLACK
            valueTextSize = 9f
            setDrawValues(true)
        }
        
        val data = BarData(dataSet)
        data.barWidth = 0.7f // Adjust bar width for better visibility
        
        binding.dailySpendingBarChart.data = data
        binding.dailySpendingBarChart.invalidate()
        
        android.util.Log.d("ReportsFragment", "Updated bar chart with ${entries.size} weekly bars")
    }

    private fun updateTimePeriodAnalysisSummary(analysis: FirebaseReportsViewModel.TimePeriodAnalysis) {
        binding.periodTotalSpentTextView.text = currencyConverter.formatAmount(analysis.totalSpent)
        binding.periodDailyAverageTextView.text = currencyConverter.formatAmount(analysis.dailyAverage)
        binding.periodTrendTextView.text = analysis.trend
        
        // Set trend color
        val trendColor = when {
            analysis.trend.contains("Rising") -> Color.RED
            analysis.trend.contains("Falling") -> Color.GREEN
            else -> Color.GRAY
        }
        binding.periodTrendTextView.setTextColor(trendColor)
    }

    private fun updateSpendingOverTimeChart(spendingData: List<FirebaseReportsViewModel.DailySpending>) {
        if (!isAdded || _binding == null) return
        
        val chart = binding.spendingOverTimeLineChart
        
        if (spendingData.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        // Create entries for the line chart
        val entries = spendingData.mapIndexed { index, dailySpending ->
            Entry(index.toFloat(), dailySpending.amount.toFloat())
        }

        // Create line dataset
        val dataSet = LineDataSet(entries, "Daily Spending").apply {
            color = Color.BLACK
            setCircleColor(Color.BLACK)
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = Color.BLACK
            fillAlpha = 30
        }

        // Set data to chart
        val lineData = LineData(dataSet)
        chart.data = lineData

        // Configure X-axis labels
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                val index = value.toInt()
                return if (index >= 0 && index < spendingData.size) {
                    spendingData[index].formattedDate
                } else {
                    ""
                }
            }
        }

        // Set X-axis range
        chart.xAxis.apply {
            axisMinimum = 0f
            axisMaximum = (spendingData.size - 1).toFloat()
            labelCount = minOf(7, spendingData.size)
        }

        // Refresh chart
        chart.notifyDataSetChanged()
        chart.invalidate()

        android.util.Log.d("ReportsFragment", "Updated spending over time chart with ${entries.size} data points")
    }

    private fun toggleCategoryDisplay() {
        showAmounts = !showAmounts
        
        // Update button text
        binding.categoryDisplayToggleButton.text = if (showAmounts) "Amount" else "Percentage"
        
        // Refresh the legend with current data
        viewModel.categorySpending.value.let { categoryData ->
            updateCategoryLegend(categoryData)
        }
        
        android.util.Log.d("ReportsFragment", "Toggled display mode to: ${if (showAmounts) "Amounts" else "Percentages"}")
    }

    private fun exportReport() {
        // Simple export functionality
        val monthYear = binding.monthYearTextView.text.toString()
        val totalSpent = binding.totalSpendingAmountTextView.text.toString()
        
        android.widget.Toast.makeText(
            requireContext(),
            "Report for $monthYear exported\nTotal Spent: $totalSpent",
            android.widget.Toast.LENGTH_LONG
        ).show()
        
        // In a real app, you'd export to PDF or share via email
        viewModel.exportReport()
    }

    private fun setupCategoryChips() {
        // Load categories and create chips
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val categories = viewModel.getAvailableCategories()
                
                // Clear existing chips except "All Categories"
                val allCategoriesChip = binding.categoryFilterChipGroup.findViewById<com.google.android.material.chip.Chip>(R.id.chipAllCategories)
                binding.categoryFilterChipGroup.removeAllViews()
                binding.categoryFilterChipGroup.addView(allCategoriesChip)
                
                // Add category chips
                categories.forEach { category ->
                    val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                        text = category
                        isCheckable = true
                        setChipBackgroundColorResource(android.R.color.transparent)
                        setTextAppearanceResource(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                        chipStrokeWidth = 2f
                    }
                    binding.categoryFilterChipGroup.addView(chip)
                }
                
                // Set up category filter listener
                binding.categoryFilterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
                    selectedCategory = if (checkedIds.isNotEmpty()) {
                        val selectedChip = binding.categoryFilterChipGroup.findViewById<com.google.android.material.chip.Chip>(checkedIds[0])
                        val categoryName = selectedChip.text.toString()
                        if (categoryName == "All Categories") null else categoryName
                    } else {
                        null
                    }
                    
                    // Reload data with new category filter
                    loadTimePeriodAnalysisWithFilters()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ReportsFragment", "Error setting up category chips", e)
            }
        }
    }

    private fun loadTimePeriodAnalysisWithFilters() {
        viewModel.loadTimePeriodAnalysis(
            period = selectedTimePeriod,
            startDate = customStartDate,
            endDate = customEndDate,
            categoryFilter = selectedCategory
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 