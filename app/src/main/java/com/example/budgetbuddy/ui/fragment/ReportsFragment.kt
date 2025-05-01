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
            setCenterTextSize(16f)
            setCenterTextColor(Color.BLACK)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            setDrawEntryLabels(false) // Disable drawing category labels on slices
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
              // Toggle category display mode
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
        dataSet.colors = colors // Use colors from state

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(binding.categoryPieChart)) // Use PieChart context here
        data.setValueTextSize(11f)
        data.setValueTextColor(Color.BLACK)
        data.setDrawValues(false) // Disable drawing values on slices

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

    private fun downloadReport() {
        val reportContent = viewModel.generateReportContent()
        if (reportContent == null) {
            Toast.makeText(context, "Cannot generate report at this time.", Toast.LENGTH_SHORT).show()
            return
        }

        val state = viewModel.uiState.value // Get current state for filename
        val filename = "BudgetReport_${state.selectedMonthYearText.replace(" ", "_")}.txt"
        val resolver = requireContext().contentResolver

        // Saving to MediaStore Downloads collection is only reliably supported on API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                // RELATIVE_PATH is the key for API 29+
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/BudgetBuddy")
            }

            // Use the specific Downloads collection URI
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
                // Clean up the MediaStore entry if saving failed
                try {
                    resolver.delete(uri, null, null)
                } catch (deleteException: Exception) {
                    Log.e("ReportsFragment", "Failed to delete MediaStore entry after save failure", deleteException)
                }
            }
        } else {
            // Fallback for API < 29
            // Option 1: Disable feature
            Log.w("ReportsFragment", "Download report feature requires Android 10 (API 29) or higher.")
            Toast.makeText(context, "Download report requires Android 10+", Toast.LENGTH_LONG).show()

            // Option 2: Implement legacy storage approach (More complex, requires permissions
            // and potentially requestLegacyExternalStorage=true in Manifest for easier implementation,
            // or using Storage Access Framework for best practice)
            /*
            // Example using legacy Downloads directory (Requires WRITE_EXTERNAL_STORAGE and maybe legacy flag)
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val budgetBuddyDir = File(downloadsDir, "BudgetBuddy")
                if (!budgetBuddyDir.exists()) {
                    budgetBuddyDir.mkdirs()
                }
                val file = File(budgetBuddyDir, filename)
                FileOutputStream(file).use {
                    it.write(reportContent.toByteArray())
                }
                 Toast.makeText(context, "Report saved to Downloads/BudgetBuddy (Legacy)", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("ReportsFragment", "Failed to save report using legacy method", e)
                Toast.makeText(context, "Failed to save report.", Toast.LENGTH_SHORT).show()
            }
            */
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