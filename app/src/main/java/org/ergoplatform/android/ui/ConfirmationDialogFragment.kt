package org.ergoplatform.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.android.databinding.FragmentConfirmationDialogBinding

/**
 * Confirmation dialog
 */

const val ARG_CONFIRMATION_TEXT = "ARG_TEXT"
const val ARG_BUTTON_YES_LABEL = "ARG_YES_LABEL"

class ConfirmationDialogFragment : BottomSheetDialogFragment() {

    private var _binding : FragmentConfirmationDialogBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentConfirmationDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvPrompt.text = arguments?.getString(ARG_CONFIRMATION_TEXT)
        binding.buttonYes.text = arguments?.getString(ARG_BUTTON_YES_LABEL)

        binding.buttonYes.setOnClickListener {
            (parentFragment as? ConfirmationCallback)?.onConfirm()
            dismiss()
        }
        binding.buttonCancel.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

interface ConfirmationCallback {
    fun onConfirm()
}