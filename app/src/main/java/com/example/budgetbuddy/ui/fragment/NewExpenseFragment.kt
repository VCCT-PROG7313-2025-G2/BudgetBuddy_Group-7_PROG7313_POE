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
import kotlinx.coroutines.flow.collectLatest
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import java.io.IOException

@AndroidEntryPoint
class NewExpenseFragment : Fragment() {

    private var _binding: FragmentNewExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewExpenseViewModel by viewModels()

    // Activity Result Launchers for picking images
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var getContentLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // Hold the date picker instance
    private var datePicker: MaterialDatePicker<Long>? = null

    // URI for the temporary camera image file
    private var cameraImageUri: Uri? = null

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

        // Initialize camera launcher
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                cameraImageUri?.let { handleSelectedImage(it) }
            } else {
                Log.e("NewExpenseFragment", "Camera capture failed or was cancelled.")
                // Optionally delete the temp file if needed, though it's in cache
                cameraImageUri = null // Reset URI
            }
        }

        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(context, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
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
        setupCategoryDropdown()
        setupDatepicker()
        setupClickListeners()
        observeViewModel()
        observeCategories()
        updateDateDisplay() // Show initial date
    }

    private fun setupCategoryDropdown() {
        // Use the same restricted list of categories as the home screen
        val categories = listOf("Food & Dining", "Transport", "Shopping", "Utilities", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.categoryAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupDatepicker() {
        // Create the date picker instance and store it
        datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // Default to today or use selectedDate
            .build()

        // Use a local val to ensure smart cast is possible inside the listener
        val picker = datePicker
        if (picker != null) {
            picker.addOnPositiveButtonClickListener { selection ->
                // Selection is UTC milliseconds at midnight. Adjust for local timezone.
                val utcMillis = selection
                val timeZone = TimeZone.getDefault()
                val offset = timeZone.getOffset(utcMillis)
                val localDate = Date(utcMillis + offset) // Create Date object using adjusted millis

                selectedDate = localDate // Update the stored Date object
                updateDateDisplay() // Update the text field
            }
        } else {
            Log.e("NewExpenseFragment", "Date picker instance was null when trying to add listener.")
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
        // Add listener to show the date picker
        binding.dateEditText.setOnClickListener {
            datePicker?.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
        }
    }

    private fun launchImagePicker() {
        // Show choice dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Attach Receipt")
            .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> launchGalleryPicker()
                }
            }
            .show()
    }

    // Separate function to launch gallery pickers
    private fun launchGalleryPicker() {
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

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                dispatchTakePictureIntent()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
                // TODO: Show rationale if needed
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            else -> {
                // Directly ask for the permission.
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        try {
            // Create a temporary file using FileProvider
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("NewExpenseFragment", "Error creating image file", ex)
                null
            }

            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider", // Match authorities in Manifest
                    it
                )
                cameraImageUri = photoURI // Store the URI for the result
                takePictureLauncher.launch(photoURI)
            }
        } catch (e: Exception) {
            Log.e("NewExpenseFragment", "Error dispatching take picture intent", e)
            Toast.makeText(context, "Could not launch camera", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = requireContext().cacheDir // Use cache directory
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
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

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collectLatest { categoryList ->
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList)
                    binding.categoryAutoCompleteTextView.setAdapter(adapter)
                    Log.d("NewExpenseFragment", "Updated categories: $categoryList")
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