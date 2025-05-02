package com.example.budgetbuddy.ui.fragment

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import android.graphics.Color
import android.content.ContentResolver
import androidx.annotation.RequiresApi

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
        // Pie Chart for Category Spending
        binding.categoryPieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setHoleRadius(58f)
            setTransparentCircleRadius(61f)
            setDrawCenterText(true)
            setCenterTextSize(16f)
            setCenterTextColor(Color.BLACK)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            setDrawEntryLabels(false)
        }

        // Bar Chart for Daily/Weekly Spending Trend
         binding.dailySpendingBarChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            axisRight.isEnabled = false

            val xAxis = xAxis
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = Color.BLACK

            val leftAxis = axisLeft
            leftAxis.setDrawGridLines(false)
            leftAxis.setDrawAxisLine(false)
            leftAxis.setDrawLabels(true)
            leftAxis.axisMinimum = 0f
            leftAxis.textColor = Color.BLACK
         }
    }

     private fun setupClickListeners() {
        binding.previousMonthNavButton.setOnClickListener {
            viewModel.changeMonth(-1)
        }
        binding.nextMonthNavButton.setOnClickListener {
            viewModel.changeMonth(1)
        }
        binding.downloadReportButton.setOnClickListener {
            downloadReport()
        }
         binding.moreOptionsButton.setOnClickListener {
              viewModel.toggleCategoryDisplayMode()
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

                    updatePieChart(state.pieChartData, state.pieChartColors, state.totalSpending)
                    updatePieLegend(state.pieChartLegend, state.pieChartColors)
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

    private fun updatePieChart(entries: List<PieEntry>, colors: List<Int>, totalSpending: BigDecimal) {
        if (entries.isEmpty()) {
            binding.categoryPieChart.clear()
            binding.categoryPieChart.centerText = "No Spending\nThis Period"
            binding.categoryPieChart.invalidate()
            return
        }

        binding.categoryPieChart.centerText = formatCurrency(totalSpending)

        val dataSet = PieDataSet(entries, "Spending Categories")
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(binding.categoryPieChart))
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.BLACK)
        data.setDrawValues(false)

        binding.categoryPieChart.data = data
        binding.categoryPieChart.highlightValues(null)
        binding.categoryPieChart.invalidate()
    }

     private fun updateBarChart(entries: List<BarEntry>, labels: List<String>) {
        if (entries.isEmpty()) {
            binding.dailySpendingBarChart.clear()
            binding.dailySpendingBarChart.invalidate()
            return
        }

        val dataSet = BarDataSet(entries, "Daily Spending")
        dataSet.color = Color.BLACK
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        binding.dailySpendingBarChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.dailySpendingBarChart.xAxis.labelCount = labels.size
        binding.dailySpendingBarChart.data = barData
        binding.dailySpendingBarChart.invalidate()
    }


    private fun updatePieLegend(legendItems: List<Pair<String, String>>, colors: List<Int>) {
        binding.categoryLegendLayout.removeAllViews()
        val inflater = LayoutInflater.from(context)

        legendItems.forEachIndexed { index, item ->
            val legendBinding = ItemReportLegendBinding.inflate(inflater, binding.categoryLegendLayout, false)

            val (categoryName, percentage) = item
            legendBinding.legendColorDot.background.setTint(colors.getOrElse(index) { Color.GRAY })
            legendBinding.legendCategoryName.text = categoryName
            legendBinding.legendPercentage.text = percentage

            binding.categoryLegendLayout.addView(legendBinding.root)
        }
    }

    private fun downloadReport() {
        val reportContent = viewModel.generateReportContent()
        if (reportContent == null) {
            Toast.makeText(context, "Cannot generate report at this time.", Toast.LENGTH_SHORT).show()
            return
        }

        val state = viewModel.uiState.value
        val filename = "BudgetReport_${state.selectedMonthYearText.replace(" ", "_")}.txt"
        val resolver = requireContext().contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveReportApi29(resolver, filename, reportContent)
        } else {
            Log.w("ReportsFragment", "Download report feature requires Android 10 (API 29) or higher.")
            Toast.makeText(context, "Download report requires Android 10+", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveReportApi29(resolver: ContentResolver, filename: String, reportContent: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/BudgetBuddy")
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri == null) {
            Log.e("ReportsFragment", "Failed to create new MediaStore record for API 29+.")
            Toast.makeText(context, "Failed to prepare download location.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(reportContent.toByteArray())
            }
            Toast.makeText(context, "Report saved to Downloads/BudgetBuddy", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Failed to save report to $uri", e)
            Toast.makeText(context, "Failed to save report.", Toast.LENGTH_SHORT).show()
            try {
                resolver.delete(uri, null, null)
            } catch (deleteException: Exception) {
                Log.e("ReportsFragment", "Failed to delete MediaStore entry after save failure", deleteException)
            }
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