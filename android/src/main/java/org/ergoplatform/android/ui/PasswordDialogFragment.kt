package org.ergoplatform.android.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.URL_FORGOT_PASSWORD_HELP
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentPasswordDialogBinding
import org.ergoplatform.appkit.SecretString

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
            binding.tvForgotPw.visibility = View.GONE
        } else {
            binding.tvForgotPw.text = HtmlCompat.fromHtml(
                "<a href=\"$URL_FORGOT_PASSWORD_HELP\">" + getString(R.string.label_forgot_password) + "</a>",
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            binding.tvForgotPw.enableLinks()
            binding.editPassword.editText?.setOnEditorActionListener(doneActionListener)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.editPassword.editText?.requestFocus()
        forceShowSoftKeyboard(requireContext())
    }

    private fun buttonDone() {
        val password = binding.editPassword.editText?.getSecretString()

        if (binding.editPasswordConfirm.visibility == View.VISIBLE) {
            val confirmPassword = binding.editPasswordConfirm.editText?.getSecretString()

            if (password == null || password != confirmPassword) {
                confirmPassword?.erase()
                password?.erase()
                binding.editPassword.error = getString(R.string.err_password_confirm)
                return
            }
            confirmPassword.erase()
        }

        val error =
            (parentFragment as? PasswordDialogCallback)?.onPasswordEntered(password)
        password?.erase()

        if (error != null)
            binding.editPassword.error = error
        else {
            hideForcedSoftKeyboard(requireContext(), binding.editPassword.editText!!)
            dismiss()
        }
    }

    private fun EditText.getSecretString(): SecretString {
        val length: Int = length()
        val pd = CharArray(length)
        text.getChars(0, length, pd, 0)
        return SecretString.create(pd)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

interface PasswordDialogCallback {
    /**
     * called by [PasswordDialogFragment]
     * @param password password. Will be erased after method returns
     * @return error message to be shown to user in case of an error
     */
    fun onPasswordEntered(password: SecretString?): String?
}