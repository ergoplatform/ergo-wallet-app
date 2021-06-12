package org.ergoplatform.android.wallet

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.NavHostFragment
import org.ergoplatform.android.MNEMONIC_WORDS_COUNT
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentRestoreWalletBinding
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.forceShowSoftKeyboard
import org.ergoplatform.android.ui.hideForcedSoftKeyboard
import org.ergoplatform.android.ui.navigateSafe

/**
 * Restores a formerly generated wallet from mnemonic
 */
class RestoreWalletFragmentDialog : FullScreenFragmentDialog() {

    private var _binding: FragmentRestoreWalletBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestoreWalletBinding.inflate(inflater, container, false)

        binding.tvMnemonic.editText?.setImeOptions(EditorInfo.IME_ACTION_DONE)
        binding.tvMnemonic.editText?.setRawInputType(InputType.TYPE_CLASS_TEXT)
        binding.tvMnemonic.editText?.setOnEditorActionListener { v, actionId, event ->
            doRestore()
            true
        }
        binding.tvMnemonic.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val words = checkMnemonic()

                if (words > 0 && words < MNEMONIC_WORDS_COUNT) {
                    binding.tvMnemonic.error = getString(
                        R.string.mnemonic_length_not_enough,
                        (MNEMONIC_WORDS_COUNT - words).toString()
                    )
                } else if (words > MNEMONIC_WORDS_COUNT) {
                    binding.tvMnemonic.error = getString(R.string.mnemonic_length_too_long)
                } else
                    binding.tvMnemonic.error = null
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        binding.buttonRestore.setOnClickListener { doRestore() }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.tvMnemonic.editText?.requestFocus()
        forceShowSoftKeyboard(requireContext())
    }

    private fun checkMnemonic(): Int {
        val mnemonic = getMnemonic()
        val words = if (mnemonic.isEmpty()) 0 else mnemonic.split(" ").size
        return words
    }

    private fun getMnemonic(): String {
        return binding.tvMnemonic.editText?.text?.trim()?.replace("\\s+".toRegex(), " ") ?: ""
    }

    private fun doRestore() {
        if (checkMnemonic() == MNEMONIC_WORDS_COUNT) {
            hideForcedSoftKeyboard(requireContext(), binding.tvMnemonic.editText!!)
            NavHostFragment.findNavController(requireParentFragment())
                .navigateSafe(
                    RestoreWalletFragmentDialogDirections.actionRestoreWalletFragmentDialogToSaveWalletFragmentDialog(
                        getMnemonic()
                    )
                )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}