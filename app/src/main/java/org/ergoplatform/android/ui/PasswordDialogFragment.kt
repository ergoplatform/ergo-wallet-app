package org.ergoplatform.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.android.databinding.FragmentPasswordDialogBinding

/**
 *
 * Shows a password input prompt and calls back the parent fragment
 */
class PasswordDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPasswordDialogBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPasswordDialogBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonDone.setOnClickListener { buttonDone() }
        binding.editPassword.editText?.setOnEditorActionListener { v, actionId, event ->
            buttonDone()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        binding.editPassword.editText?.requestFocus()
        forceShowSoftKeyboard(requireContext())
    }

    private fun buttonDone() {
        val error =
            (parentFragment as? PasswordDialogCallback)?.onPasswordEntered(binding.editPassword.editText?.text?.toString())

        if (error != null)
            binding.editPassword.error = error
        else {
            hideForcedSoftKeyboard(requireContext(), binding.editPassword.editText!!)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

interface PasswordDialogCallback {
    fun onPasswordEntered(password: String?): String?
}