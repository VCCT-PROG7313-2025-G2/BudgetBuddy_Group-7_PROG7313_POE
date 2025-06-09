package com.example.budgetbuddy.ui.dialog

import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentReceiptDialogBinding
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ReceiptDialogFragment : DialogFragment() {

    private var _binding: FragmentReceiptDialogBinding? = null
    private val binding get() = _binding!!

    private var receiptUri: Uri? = null
    
    // Touch handling for zoom/pan
    private var matrix = Matrix()
    private var savedMatrix = Matrix()
    private var start = PointF()
    private var mid = PointF()
    private var mode = NONE
    private var minScale = 1f
    private var maxScale = 4f
    private var scaleDetector: ScaleGestureDetector? = null

    companion object {
        const val TAG = "ReceiptDialog"
        private const val ARG_RECEIPT_URI = "receipt_uri"
        
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2

        fun newInstance(receiptUri: Uri): ReceiptDialogFragment {
            val fragment = ReceiptDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_RECEIPT_URI, receiptUri)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            receiptUri = it.getParcelable(ARG_RECEIPT_URI)
        }
        // Set fullscreen style for better image viewing
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize scale detector
        scaleDetector = ScaleGestureDetector(requireContext(), ScaleListener())
        
        receiptUri?.let { uri ->
            Glide.with(this)
                .load(uri)
                .error(R.drawable.ic_broken_image) // Placeholder for error
                .into(binding.receiptImageView)
        } ?: run {
            // Handle case where URI is null (optional)
            binding.receiptImageView.setImageResource(R.drawable.ic_broken_image)
        }

        // Set up touch listeners for zooming and panning
        binding.receiptImageView.setOnTouchListener { _, event ->
            scaleDetector?.onTouchEvent(event)
            
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    start.set(event.x, event.y)
                    mode = DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    mode = ZOOM
                    savedMatrix.set(matrix)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG) {
                        matrix.set(savedMatrix)
                        matrix.postTranslate(event.x - start.x, event.y - start.y)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE
                }
            }
            
            binding.receiptImageView.imageMatrix = matrix
            true
        }

        binding.closeButton.setOnClickListener {
            dismiss() // Close the dialog
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val currentScale = getMatrixScale(matrix)
            val newScale = currentScale * scaleFactor
            
            if (newScale in minScale..maxScale) {
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                binding.receiptImageView.imageMatrix = matrix
            }
            return true
        }
    }

    private fun getMatrixScale(matrix: Matrix): Float {
        val values = FloatArray(9)
        matrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    override fun onStart() {
        super.onStart()
        // Set dialog to match parent for fullscreen viewing
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 