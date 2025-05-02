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

// Marks this Fragment for Hilt injection.
@AndroidEntryPoint
class NewExpenseFragment : Fragment() {

    // --- View Binding --- 
    private var _binding: FragmentNewExpenseBinding? = null
    private val binding get() = _binding!!

    // --- ViewModel --- 
    private val viewModel: NewExpenseViewModel by viewModels()

    // --- Activity Result Launchers (Handle results from other Activities/System apps) --- 
    // For picking media (modern approach).
    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>
    // For picking general content (fallback).
    private lateinit var getContentLauncher: ActivityResultLauncher<String>
    // For taking a picture with the camera.
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    // For requesting permissions (like camera).
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // --- Date Picker Instance --- 
    private var datePicker: MaterialDatePicker<Long>? = null

    // --- State Variables --- 
    // Holds the URI of the image taken by the camera (temporary).
    private var cameraImageUri: Uri? = null
    // Holds the currently selected date for the expense.
    private var selectedDate: Date = Date() // Initialize with today's date.
    // Holds the path to the receipt image after it's copied locally.
    private var copiedReceiptPath: String? = null

    // Called when the Fragment is first created (before the view).
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Activity Result Launchers here.
        // Modern image picker.
        pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { handleSelectedImage(it) } // If an image is picked, handle it.
        }
        // Fallback image picker.
        getContentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleSelectedImage(it) }
        }
        // Camera result handler.
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                cameraImageUri?.let { handleSelectedImage(it) } // If picture taken, handle the stored URI.
            } else {
                Log.e("NewExpenseFragment", "Camera capture failed or was cancelled.")
                cameraImageUri = null // Reset if camera failed.
            }
        }
        // Permission result handler.
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                dispatchTakePictureIntent() // If permission granted, launch camera.
            } else {
                Toast.makeText(context, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Creates the Fragment's view.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Called after the view is created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // setupCategoryDropdown() // Categories are now loaded dynamically.
        setupDatepicker()       // Configure the date picker dialog.
        setupClickListeners()   // Set up button actions.
        observeViewModel()      // Start observing ViewModel for success/error states.
        observeCategories()     // Start observing ViewModel for the category list.
        updateDateDisplay()     // Show the initial date in the text field.
    }

    // Observes the dynamic category list from the ViewModel.
    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collectLatest { categoryList ->
                    // Create an adapter with the latest category list.
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList)
                    // Set the adapter on the AutoCompleteTextView.
                    binding.categoryAutoCompleteTextView.setAdapter(adapter)
                    Log.d("NewExpenseFragment", "Updated categories: $categoryList")
                }
            }
        }
    }

    // Configures the Material Date Picker dialog.
    private fun setupDatepicker() {
        // Build the date picker dialog.
        datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // Default to today.
            .build()

        // Add listener for when the user clicks "OK".
        datePicker?.addOnPositiveButtonClickListener { selection ->
            // Convert the selected UTC milliseconds to a local Date object.
            val utcMillis = selection
            val timeZone = TimeZone.getDefault()
            val offset = timeZone.getOffset(utcMillis)
            val localDate = Date(utcMillis + offset)
            selectedDate = localDate // Store the selected date.
            updateDateDisplay() // Update the text field.
        }
    }

    // Updates the date text field with the currently selected date.
    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.dateEditText.setText(dateFormat.format(selectedDate))
    }

    // Sets up actions for button clicks.
    private fun setupClickListeners() {
        // Save button: Trigger the save process.
        binding.saveExpenseButton.setOnClickListener {
            saveExpense()
        }
        // Attach receipt button: Show options to take photo or choose from gallery.
        binding.attachReceiptButton.setOnClickListener {
            launchImagePicker()
        }
        // Back button: Navigate back to the previous screen.
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        // Receipt preview: Clear the selected receipt.
        binding.receiptPreviewImageView.setOnClickListener {
            clearReceiptPreview()
        }
        // Date text field: Show the date picker dialog.
        binding.dateEditText.setOnClickListener {
            datePicker?.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
        }
    }

    // Shows a dialog asking the user to choose Camera or Gallery.
    private fun launchImagePicker() {
        AlertDialog.Builder(requireContext())
            .setTitle("Attach Receipt")
            .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch() // Camera selected
                    1 -> launchGalleryPicker()         // Gallery selected
                }
            }
            .show()
    }

    // Launches the appropriate gallery picker (modern or fallback).
    private fun launchGalleryPicker() {
        try {
            // Try the modern photo picker first.
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            Log.e("NewExpenseFragment", "PickVisualMedia failed, falling back to GetContent", e)
            try {
                // If modern picker fails, try the older GetContent method.
                getContentLauncher.launch("image/*")
            } catch (e2: Exception) {
                Log.e("NewExpenseFragment", "GetContent also failed", e2)
                Toast.makeText(context, "Cannot open image picker", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Checks if camera permission is granted. If not, requests it.
    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, launch camera.
                dispatchTakePictureIntent()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                // Explain why permission is needed (optional), then request.
                // TODO: Show rationale dialog if desired.
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
            else -> {
                // Request the permission directly.
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    // Creates an intent to launch the device camera app.
    private fun dispatchTakePictureIntent() {
        try {
            // Create a temporary file to store the camera image.
            val photoFile: File? = try { createImageFile() } catch (ex: IOException) { null }

            photoFile?.also {
                // Get a content URI for the temporary file using FileProvider.
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider", // Must match authorities in AndroidManifest.xml
                    it
                )
                cameraImageUri = photoURI // Store this URI to access the image later.
                // Launch the camera app, telling it to save the image to the provided URI.
                takePictureLauncher.launch(photoURI)
            }
        } catch (e: Exception) {
            Log.e("NewExpenseFragment", "Error dispatching take picture intent", e)
            Toast.makeText(context, "Could not launch camera", Toast.LENGTH_SHORT).show()
        }
    }

    // Creates a temporary image file in the app's cache directory.
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = requireContext().cacheDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    // Handles the image URI received from the camera or gallery.
    private fun handleSelectedImage(uri: Uri) {
        // Copy the selected image to our app's internal storage for persistent access.
        copiedReceiptPath = copyImageToInternalStorage(uri)
        if (copiedReceiptPath != null) {
            // If copy successful, show the image preview.
            binding.receiptPreviewImageView.isVisible = true
            Glide.with(this).load(copiedReceiptPath).centerCrop().into(binding.receiptPreviewImageView)
            Log.d("NewExpenseFragment", "Receipt attached: $copiedReceiptPath")
        } else {
            // If copy failed, show an error and clear any previous preview.
            Toast.makeText(context, "Failed to process receipt image", Toast.LENGTH_SHORT).show()
            clearReceiptPreview()
        }
    }

    // Copies image data from a source URI (gallery/camera) to a new file in the app's cache.
    // Returns the absolute path of the newly created file, or null on failure.
    private fun copyImageToInternalStorage(uri: Uri): String? {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val timestamp = System.currentTimeMillis()
            val fileName = "receipt_$timestamp.jpg"
            val outputFile = File(requireContext().cacheDir, fileName) // Save in cache
            outputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream) // Perform the copy
            return outputFile.absolutePath // Return the path to the copied file
        } catch (e: Exception) {
            Log.e("CopyImage", "Error copying image to internal storage", e)
            return null
        } finally {
            // Ensure streams are closed.
            inputStream?.close()
            outputStream?.close()
        }
    }

    // Clears the receipt image preview and the stored path.
    private fun clearReceiptPreview() {
        binding.receiptPreviewImageView.setImageDrawable(null)
        binding.receiptPreviewImageView.isVisible = false
        copiedReceiptPath = null
        Log.d("NewExpenseFragment", "Receipt preview cleared.")
    }

    // Gathers data from input fields and tells the ViewModel to save the expense.
    private fun saveExpense() {
        // Get amount, validate it's a number.
        val amountText = binding.amountEditText.text.toString()
        val amount = try { BigDecimal(amountText) } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
            return
        }
        // Get category and description.
        val category = binding.categoryAutoCompleteTextView.text.toString()
        val description = binding.descriptionEditText.text.toString()

        // Basic validation (optional but recommended).
        if (amount <= BigDecimal.ZERO) {
             Toast.makeText(context, "Amount must be positive", Toast.LENGTH_SHORT).show()
             return
        }
        if (category.isBlank()) {
            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Call ViewModel to save.
        viewModel.saveExpense(amount, category, selectedDate, description, copiedReceiptPath)
    }

    // Observes the UI state from the ViewModel (Success/Error/Loading).
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Disable save button while loading.
                    binding.saveExpenseButton.isEnabled = state !is NewExpenseUiState.Loading
                    // TODO: Show a visual loading indicator.

                    when (state) {
                        is NewExpenseUiState.Success -> {
                            // Show success message, navigate back, and reset ViewModel state.
                            Toast.makeText(context, "Expense saved successfully!", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                            viewModel.resetState()
                        }
                        is NewExpenseUiState.Error -> {
                            // Show error message and reset ViewModel state.
                            Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        else -> Unit // Idle or Loading state.
                    }
                }
            }
        }
    }

    // Hides the main activity toolbar.
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    // Restores the toolbar when leaving (commented out to fix image picker issue).
    override fun onPause() {
        super.onPause()
        // (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    // Cleans up View Binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 