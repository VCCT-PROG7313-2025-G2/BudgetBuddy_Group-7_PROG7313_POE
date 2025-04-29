package com.example.budgetbuddy.ui.fragment

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentNewExpenseBinding
import com.example.budgetbuddy.ui.viewmodel.NewExpenseUiState
import com.example.budgetbuddy.ui.viewmodel.NewExpenseViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.view.isVisible

@AndroidEntryPoint
class NewExpenseFragment : Fragment() {

    private var _binding: FragmentNewExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewExpenseViewModel by viewModels()

    // Activity Result Launchers for picking images
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var getContentLauncher: ActivityResultLauncher<String>

    private var selectedDate: Date = Date() // Store selected date as a Date object
    private var copiedReceiptPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Activity Result Launchers
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { handleSelectedImage(it) }
        }

        getContentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleSelectedImage(it) }
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
        setupDatePickerClickListener()
        setupClickListeners()
        observeViewModel()
        updateDateDisplay() // Show initial date
    }

    private fun setupCategorySpinner() {
        // Use the same restricted list of categories as the home screen
        val categories = listOf("Food & Dining", "Transport", "Shopping", "Utilities", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.categoryAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupDatePickerClickListener() {
        binding.dateEditText.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                 // Set initial selection to the currently selected date (converted to UTC milliseconds)
                 // Note: MaterialDatePicker uses UTC midnight milliseconds.
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // Default to today or use selectedDate
                // .setSelection(selectedDate.time + TimeZone.getDefault().getOffset(selectedDate.time)) // More accurate initial selection
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                // Selection is UTC milliseconds at midnight. Adjust for local timezone.
                val utcMillis = selection
                val timeZone = TimeZone.getDefault()
                val offset = timeZone.getOffset(utcMillis)
                val localDate = Date(utcMillis + offset) // Create Date object using adjusted millis

                selectedDate = localDate // Update the stored Date object
                updateDateDisplay() // Update the text field
            }

            datePicker.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
        }
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Common format
        binding.dateEditText.setText(dateFormat.format(selectedDate))
    }

    private fun setupClickListeners() {
        binding.saveExpenseButton.setOnClickListener {
            saveExpense()
        }
        binding.attachReceiptButton.setOnClickListener {
            launchImagePicker()
        }
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.receiptPreviewImageView.setOnClickListener {
            clearReceiptPreview()
        }
    }

    private fun launchImagePicker() {
        try {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            Log.e("NewExpenseFragment", "PickVisualMedia failed, falling back to GetContent", e)
            try {
                getContentLauncher.launch("image/*")
            } catch (e2: Exception) {
                Log.e("NewExpenseFragment", "GetContent also failed", e2)
                Toast.makeText(context, "Cannot open image picker", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        copiedReceiptPath = copyImageToInternalStorage(uri)
        if (copiedReceiptPath != null) {
            binding.receiptPreviewImageView.isVisible = true
            Glide.with(this)
                .load(copiedReceiptPath)
                .centerCrop()
                .into(binding.receiptPreviewImageView)
            Log.d("NewExpenseFragment", "Receipt attached: $copiedReceiptPath")
        } else {
            Toast.makeText(context, "Failed to process receipt image", Toast.LENGTH_SHORT).show()
            clearReceiptPreview()
        }
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val timestamp = System.currentTimeMillis()
            val fileName = "receipt_$timestamp.jpg"
            val outputFile = File(requireContext().cacheDir, fileName)
            outputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream)
            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e("CopyImage", "Error copying image to internal storage", e)
            return null
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    private fun clearReceiptPreview() {
        binding.receiptPreviewImageView.setImageDrawable(null)
        binding.receiptPreviewImageView.isVisible = false
        copiedReceiptPath = null
        Log.d("NewExpenseFragment", "Receipt preview cleared.")
    }

    private fun saveExpense() {
        val amountText = binding.amountEditText.text.toString()
        val amount = try {
            BigDecimal(amountText)
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val category = binding.categoryAutoCompleteTextView.text.toString()
        val description = binding.descriptionEditText.text.toString()

        viewModel.saveExpense(amount, category, selectedDate, description, copiedReceiptPath)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.saveExpenseButton.isEnabled = state !is NewExpenseUiState.Loading
                    // TODO: Show loading indicator

                    when (state) {
                        is NewExpenseUiState.Success -> {
                            Toast.makeText(context, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp() // Go back
                            viewModel.resetState()
                        }
                        is NewExpenseUiState.Error -> {
                            Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        else -> Unit // Idle or Loading
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        // Removed code that showed the action bar on pause to prevent it flashing during image picking
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 