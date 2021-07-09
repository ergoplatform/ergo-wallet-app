package org.ergoplatform.android.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentPasswordDialogBinding

const val ARG_SHOW_CONFIRMATION = "ARG_SHOW_CONFIRMATION"

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
        val doneActionListener: (v: TextView, actionId: Int, event: KeyEvent?) -> Boolean =
            { _, _, _ ->
                buttonDone()
                true
            }

        if (arguments?.getBoolean(ARG_SHOW_CONFIRMATION) == true) {
            binding.editPasswordConfirm.visibility = View.VISIBLE
            binding.editPasswordConfirm.editText?.setOnEditorActionListener(doneActionListener)
            binding.editPassword.editText?.imeOptions = EditorInfo.IME_ACTION_NEXT
        } else {
            binding.editPassword.editText?.setOnEditorActionListener(doneActionListener)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.editPassword.editText?.requestFocus()
        forceShowSoftKeyboard(requireContext())
    }

    private fun buttonDone() {
        val password = binding.editPassword.editText?.text?.toString()

        if (binding.editPasswordConfirm.visibility == View.VISIBLE) {
            val confirmPassword = binding.editPasswordConfirm.editText?.text?.toString()

            if (password == null || !password.equals(confirmPassword)) {
                binding.editPassword.error = getString(R.string.err_password_confirm)
                return
            }
        }

        val error =
            (parentFragment as? PasswordDialogCallback)?.onPasswordEntered(password)

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