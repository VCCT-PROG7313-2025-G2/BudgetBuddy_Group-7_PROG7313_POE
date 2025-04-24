package com.example.budgetbuddy.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.budgetbuddy.databinding.FragmentReportsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dateRangeButton.setOnClickListener {
            // TODO: Implement date range selection logic (similar to ExpensesFragment)
            Toast.makeText(context, "Date range selection not implemented", Toast.LENGTH_SHORT).show()
        }

        // TODO: Load report data from ViewModel
        // TODO: Setup Pie Chart (e.g., using MPAndroidChart)
        // TODO: Setup Line Chart (e.g., using MPAndroidChart)
        // TODO: Populate Key Stats TextViews
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 