package org.ergoplatform.android.wallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import org.ergoplatform.android.MNEMONIC_MIN_WORDS_COUNT
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentRestoreWalletBinding
import org.ergoplatform.android.loadAppKitMnemonicWordList
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.forceShowSoftKeyboard
import org.ergoplatform.android.ui.hideForcedSoftKeyboard
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.appkit.Mnemonic
import org.ergoplatform.appkit.MnemonicValidationException
import java.util.*

/**
 * Restores a formerly generated wallet from mnemonic
 */
class RestoreWalletFragmentDialog : FullScreenFragmentDialog() {

    private var _binding: FragmentRestoreWalletBinding? = null
    private val binding get() = _binding!!

    private val wordList = loadAppKitMnemonicWordList()
    private var isSecondButtonClick = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRestoreWalletBinding.inflate(inflater, container, false)

        binding.tvMnemonic.editText?.setOnEditorActionListener { _, _, _ ->
            doRestore()
            true
        }
        binding.tvMnemonic.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                isSecondButtonClick = false
                val words = countMnemonicWords()

                // check for invalid words, but skip the last one when it is not completed yet
                val hasInvalidWords = words > 0 && mnemonicToWords(getMnemonic())
                    .dropLast(if (s.toString().endsWith(" ")) 0 else 1)
                    .filter { word -> Collections.binarySearch(wordList, word) < 0 }
                    .isNotEmpty()

                if (hasInvalidWords) {
                    binding.tvMnemonic.error = getString(R.string.mnemonic_unknown_words)
                } else if (words > 0 && words < MNEMONIC_MIN_WORDS_COUNT) {
                    binding.tvMnemonic.error = getString(
                        R.string.mnemonic_length_not_enough,
                        (MNEMONIC_MIN_WORDS_COUNT - words).toString()
                    )
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

    private fun countMnemonicWords(): Int {
        val mnemonic = getMnemonic()
        val words = if (mnemonic.isEmpty()) 0 else mnemonicToWords(mnemonic).size
        return words
    }

    private fun getMnemonic(): String {
        return binding.tvMnemonic.editText?.text?.trim()?.replace("\\s+".toRegex(), " ") ?: ""
    }

    private fun mnemonicToWords(mnemonic: String): List<String> {
        return mnemonic.split(" ")
    }

    private fun doRestore() {
        val wordsCount = countMnemonicWords()
        if (wordsCount >= MNEMONIC_MIN_WORDS_COUNT) {
            hideForcedSoftKeyboard(requireContext(), binding.tvMnemonic.editText!!)

            val mnemonic = getMnemonic()
            var mnemonicIsValid = true

            try {
                Mnemonic.checkEnglishMnemonic(mnemonicToWords(mnemonic))
            } catch (e: MnemonicValidationException) {
                mnemonicIsValid = false
            }

            if (!isSecondButtonClick && !mnemonicIsValid) {
                isSecondButtonClick = true
                binding.tvMnemonic.error = getString(R.string.mnemonic_invalid)
            } else {
                NavHostFragment.findNavController(requireParentFragment())
                    .navigateSafe(
                        RestoreWalletFragmentDialogDirections.actionRestoreWalletFragmentDialogToSaveWalletFragmentDialog(
                            mnemonic
                        )
                    )
            }
        } else {
            binding.tvMnemonic.error = getString(
                R.string.mnemonic_length_not_enough,
                (MNEMONIC_MIN_WORDS_COUNT - wordsCount).toString()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}