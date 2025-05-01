package com.example.budgetbuddy.ui.dialog

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.budgetbuddy.R
import com.example.budgetbuddy.databinding.FragmentReceiptDialogBinding

class ReceiptDialogFragment : DialogFragment() {

    private var _binding: FragmentReceiptDialogBinding? = null
    private val binding get() = _binding!!

    private var receiptUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            receiptUri = it.getParcelable(ARG_RECEIPT_URI)
        }
        // Optional: Set a style for the dialog (e.g., no title)
        // setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth)
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

        receiptUri?.let { uri ->
            Glide.with(this)
                .load(uri)
                .error(R.drawable.ic_broken_image) // Placeholder for error
                .into(binding.receiptImageView)
        } ?: run {
            // Handle case where URI is null (optional)
            binding.receiptImageView.setImageResource(R.drawable.ic_broken_image)
        }

        binding.closeButton.setOnClickListener {
            dismiss() // Close the dialog
        }
    }

    override fun onStart() {
        super.onStart()
        // Optional: Set dialog dimensions (e.g., wrap content or specific size)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ReceiptDialog"
        private const val ARG_RECEIPT_URI = "receipt_uri"

        fun newInstance(receiptUri: Uri): ReceiptDialogFragment {
            val fragment = ReceiptDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_RECEIPT_URI, receiptUri)
            fragment.arguments = args
            return fragment
        }
    }
} 