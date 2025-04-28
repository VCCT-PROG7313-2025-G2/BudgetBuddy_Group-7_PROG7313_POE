package com.example.budgetbuddy.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentReportsBinding
import com.example.budgetbuddy.databinding.ItemReportLegendBinding
import com.example.budgetbuddy.ui.viewmodel.ReportsViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharts()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupCharts() {
        // Basic Pie Chart Setup
        binding.categoryPieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = false // Using custom legend
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT) // Use method setHoleColor
            setHoleRadius(58f)
            setTransparentCircleRadius(61f)
            setDrawCenterText(true)
            centerText = "Spending" // TODO: Make dynamic?
            setCenterTextSize(16f)
            setCenterTextColor(Color.BLACK)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
        }

        // Basic Bar Chart Setup
         binding.dailySpendingBarChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            axisRight.isEnabled = false // Disable right Y axis

            val xAxis = xAxis
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f // only intervals of 1 day
            xAxis.textColor = Color.BLACK

            val leftAxis = axisLeft
            leftAxis.setDrawGridLines(false)
            leftAxis.setDrawAxisLine(false)
            leftAxis.setDrawLabels(true) // Show Y-axis labels (spending amount)
            leftAxis.axisMinimum = 0f // start at zero
            leftAxis.textColor = Color.BLACK
         }
    }

     private fun setupClickListeners() {
        binding.previousMonthButton.setOnClickListener {
            viewModel.changeMonth(-1)
        }
        binding.nextMonthButton.setOnClickListener {
            viewModel.changeMonth(1)
        }
        binding.downloadReportButton.setOnClickListener {
            // TODO: Implement report download functionality
            Toast.makeText(context, "Download Report (Not Implemented)", Toast.LENGTH_SHORT).show()
        }
         binding.moreOptionsButton.setOnClickListener {
             // TODO: Implement more options menu (e.g., filter, date range)
            Toast.makeText(context, "More Options (Not Implemented)", Toast.LENGTH_SHORT).show()
         }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.monthYearTextView.text = state.selectedMonthYearText
                    binding.totalSpendingAmountTextView.text = formatCurrency(state.totalSpending)
                    binding.spendingChangeTextView.text = state.spendingChangeText
                    // TODO: Update spending change icon based on text/value

                    updatePieChart(state.pieChartData, state.pieChartColors)
                    updatePieLegend(state.pieChartLegend, state.pieChartColors) // Pass colors for legend dots
                     state.barChartData?.let { (entries, labels) ->
                        updateBarChart(entries, labels)
                    } ?: binding.dailySpendingBarChart.clear()


                    // Handle loading and error states if needed
                    // binding.progressBar.isVisible = state.isLoading
                    state.error?.let {
                        Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun updatePieChart(entries: List<PieEntry>, colors: List<Int>) {
        if (entries.isEmpty()) {
            binding.categoryPieChart.clear()
            binding.categoryPieChart.centerText = "No spending data"
            binding.categoryPieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Spending Categories")
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.colors = colors // Use colors from state

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(binding.categoryPieChart)) // Use PieChart context here
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.BLACK)

        binding.categoryPieChart.data = data
        binding.categoryPieChart.highlightValues(null) // Unhighlight everything
        binding.categoryPieChart.invalidate() // Refresh chart
    }

     private fun updateBarChart(entries: List<BarEntry>, labels: List<String>) {
        if (entries.isEmpty()) {
            binding.dailySpendingBarChart.clear()
            binding.dailySpendingBarChart.invalidate()
            return
        }

        val dataSet = BarDataSet(entries, "Daily Spending")
        // Use a single color or a color template
        dataSet.color = Color.BLACK // Use standard black for now
        dataSet.setDrawValues(false) // Don't draw values on top of bars

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f // Adjust bar width

        binding.dailySpendingBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.dailySpendingBarChart.xAxis.labelCount = labels.size // Ensure all labels are considered
        binding.dailySpendingBarChart.data = barData
        binding.dailySpendingBarChart.invalidate() // Refresh chart
    }


    private fun updatePieLegend(legendItems: List<Pair<String, String>>, colors: List<Int>) {
        binding.categoryLegendLayout.removeAllViews() // Clear previous legend items
        val inflater = LayoutInflater.from(context)

        legendItems.forEachIndexed { index, item ->
            // Inflate the legend item layout (NEED TO CREATE item_report_legend.xml)
            val legendBinding = ItemReportLegendBinding.inflate(inflater, binding.categoryLegendLayout, false)

            val (categoryName, percentage) = item
            legendBinding.legendColorDot.background.setTint(colors.getOrElse(index) { Color.GRAY }) // Set dot color
            legendBinding.legendCategoryName.text = categoryName
            legendBinding.legendPercentage.text = percentage

            binding.categoryLegendLayout.addView(legendBinding.root)
        }
    }

    private fun formatCurrency(amount: BigDecimal): String {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)
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