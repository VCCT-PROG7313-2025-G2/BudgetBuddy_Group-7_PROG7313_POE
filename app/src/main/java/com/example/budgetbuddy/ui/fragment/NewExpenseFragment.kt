package com.example.budgetbuddy.ui.fragment

import android.app.Activity.RESULT_OK
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentNewExpenseBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class NewExpenseFragment : Fragment() {

    private var _binding: FragmentNewExpenseBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: Calendar = Calendar.getInstance()
    private var receiptUri: Uri? = null

    // Activity Result Launcher for picking image
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            receiptUri = result.data?.data
            binding.receiptPreviewImageView.setImageURI(receiptUri)
            binding.receiptPreviewImageView.visibility = View.VISIBLE
            // TODO: Handle potential errors loading image
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategorySpinner()
        setupDatePicker()
        setupReceiptPicker()

        binding.saveExpenseButton.setOnClickListener {
            saveExpense()
        }
    }

    private fun setupCategorySpinner() {
        // TODO: Replace with actual categories from ViewModel/Repository
        val categories = arrayOf("Food & Dining", "Housing", "Transport", "Shopping", "Utilities", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.categoryAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        updateDateInView()

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            selectedDate.set(Calendar.YEAR, year)
            selectedDate.set(Calendar.MONTH, monthOfYear)
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        binding.dateEditText.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                dateSetListener,
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
         // Also trigger for the layout click
        binding.dateInputLayout.setEndIconOnClickListener {
             binding.dateEditText.performClick()
        }
    }

    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd" // Define date format
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.dateEditText.setText(sdf.format(selectedDate.time))
    }

    private fun setupReceiptPicker() {
        binding.attachReceiptButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }
    }

    private fun saveExpense() {
        val amountStr = binding.amountEditText.text.toString()
        val category = binding.categoryAutoCompleteTextView.text.toString()
        val notes = binding.notesEditText.text.toString()
        val date = binding.dateEditText.text.toString() // Already formatted string

        if (amountStr.isBlank() || category.isBlank()) {
            Toast.makeText(context, "Amount and Category are required.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
             Toast.makeText(context, "Please enter a valid positive amount.", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Implement actual saving logic using ViewModel/Repository
        // Include amount, date, category, notes, receiptUri (if available)
        println("Saving Expense:")
        println(" Amount: $amount")
        println(" Date: $date")
        println(" Category: $category")
        println(" Notes: $notes")
        println(" Receipt URI: $receiptUri")

        Toast.makeText(context, "Expense Saved (Not implemented)", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp() // Go back to the previous screen (Home)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 