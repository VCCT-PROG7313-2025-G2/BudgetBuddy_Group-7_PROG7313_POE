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

    // --- Properties ---
    private var _binding: FragmentNewExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewExpenseViewModel by viewModels()

    // Activity Result Launchers for picking/capturing images
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var getContentLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // Hold the date picker instance
    private var datePicker: MaterialDatePicker<Long>? = null

    // URI for the temporary camera image file
    private var cameraImageUri: Uri? = null

    // State variables
    private var selectedDate: Date = Date() // Store selected date as a Date object
    private var copiedReceiptPath: String? = null // Store path to the *copied* receipt image in internal storage

    // --- Fragment Lifecycle Methods ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeActivityLaunchers()
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

    // --- Initialization ---
    private fun initializeActivityLaunchers() {
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

    // --- UI Setup ---
    private fun setupCategoryDropdown() {
        // Use the same restricted list of categories as the home screen
        // TODO: Ideally, fetch categories from a shared source or ViewModel
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

    private fun setupClickListeners() {
        binding.saveExpenseButton.setOnClickListener {
            saveExpense()
        }
        binding.attachReceiptButton.setOnClickListener {
            showImageSourceDialog()
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

    // --- ViewModel Observation ---
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.saveExpenseButton.isEnabled = state !is NewExpenseUiState.Loading
                    // binding.loadingIndicator.isVisible = state is NewExpenseUiState.Loading

                    when (state) {
                        is NewExpenseUiState.Success -> {
                            Toast.makeText(context, "Expense saved!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp() // Go back after successful save
                            viewModel.resetState()
                        }
                        is NewExpenseUiState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        else -> Unit // Idle or Loading
                    }
                }
            }
        }
    }

    private fun observeCategories() {
        // Example: Observe categories from ViewModel if dynamic
        // viewModel.categories.collectLatest { categories ->
        //    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        //    binding.categoryAutoCompleteTextView.setAdapter(adapter)
        // }
    }

    // --- Core Logic ---
    private fun saveExpense() {
        val amountText = binding.amountEditText.text.toString()
        val category = binding.categoryAutoCompleteTextView.text.toString()
        val notes = binding.notesEditText.text.toString().trim()

        // Basic Validation
        if (amountText.isBlank() || category.isBlank()) {
            Toast.makeText(context, "Amount and Category are required", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = try {
            BigDecimal(amountText)
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid amount format", Toast.LENGTH_SHORT).show()
            return@saveExpense // Return explicitly from lambda
        }

        // Pass data to ViewModel
        viewModel.saveExpense(amount, selectedDate, category, notes, copiedReceiptPath)
    }

    // --- Image Handling ---
    private fun showImageSourceDialog() {
        // Renamed from launchImagePicker for clarity
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

    private fun launchGalleryPicker() {
        // Use the modern Photo Picker first
        try {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            Log.e("NewExpenseFragment", "PickVisualMedia failed, falling back to GetContent", e)
            // Fallback to GetContent if Photo Picker is not available or fails
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
                // Permission is already granted
                dispatchTakePictureIntent()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                // Explain why the permission is needed (optional, good practice)
                // Show rationale dialog here... then call requestPermissionLauncher
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA) // Request after showing rationale
            }
            else -> {
                // Directly ask for the permission.
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name (e.g., using timestamp)
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir("receipts") // Use app-specific external storage
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs() // Create directory if it doesn't exist
        }
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file path for use with ACTION_VIEW intents if needed
             // currentPhotoPath = absolutePath
        }
    }


    private fun dispatchTakePictureIntent() {
        try {
            // Create a temporary file using FileProvider
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("NewExpenseFragment", "Error creating image file", ex)
                Toast.makeText(context, "Error preparing camera", Toast.LENGTH_SHORT).show()
                null
            }

            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider", // Match authorities in Manifest
                    it
                )
                cameraImageUri = photoURI // Store the URI for the result handler
                takePictureLauncher.launch(photoURI)
            }
        } catch (e: Exception) {
            Log.e("NewExpenseFragment", "Error dispatching take picture intent", e)
            Toast.makeText(context, "Could not launch camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSelectedImage(sourceUri: Uri) {
        // Copy the selected image to internal storage for persistent access
        copiedReceiptPath = copyImageToInternalStorage(sourceUri)

        if (copiedReceiptPath != null) {
            // Load the *copied* image into the preview
            Glide.with(this)
                .load(copiedReceiptPath) // Load from the internal path
                .error(R.drawable.ic_broken_image) // Optional error placeholder
                .into(binding.receiptPreviewImageView)
            binding.receiptPreviewImageView.isVisible = true
        } else {
            Toast.makeText(context, "Failed to attach receipt", Toast.LENGTH_SHORT).show()
            clearReceiptPreview() // Clear preview if copy failed
        }
    }

    private fun copyImageToInternalStorage(sourceUri: Uri): String? {
        val context = requireContext()
        val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
        if (inputStream == null) {
            Log.e("NewExpenseFragment", "Failed to get input stream from URI: $sourceUri")
            return null
        }

        // Create a unique filename (e.g., using timestamp)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "receipt_$timeStamp.jpg"
        val outputFile = File(context.filesDir, fileName) // Store in app's internal files directory

        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream)
            Log.d("NewExpenseFragment", "Receipt copied to: ${outputFile.absolutePath}")
            return outputFile.absolutePath // Return the path of the copied file
        } catch (e: Exception) {
            Log.e("NewExpenseFragment", "Failed to copy receipt image", e)
            outputFile.delete() // Clean up partially copied file on error
            return null
        } finally {
            try {
                inputStream.close()
                outputStream?.close()
            } catch (e: IOException) {
                Log.e("NewExpenseFragment", "Error closing streams", e)
            }
        }
    }

    private fun clearReceiptPreview() {
        binding.receiptPreviewImageView.setImageDrawable(null) // Clear the image
        binding.receiptPreviewImageView.isVisible = false
        copiedReceiptPath = null // Clear the stored path
        // Optionally, delete the copied file if it's no longer needed immediately
        // File(copiedReceiptPath).delete()
    }

    // --- UI Helper Functions ---
    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Use a consistent format
        binding.dateEditText.setText(dateFormat.format(selectedDate))
    }

} 
} 