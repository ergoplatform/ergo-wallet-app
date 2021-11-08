package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import org.ergoplatform.MNEMONIC_WORDS_COUNT
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentConfirmCreateWalletBinding
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.navigateSafe
import kotlin.random.Random

/**
 * Create new wallet, step 2:
 * User confirms creation of wallet by entering two mnemonic words and some checkboxes
 */
class ConfirmCreateWalletFragment : FullScreenFragmentDialog() {

    private var _binding: FragmentConfirmCreateWalletBinding? = null
    private val binding get() = _binding!!

    private val args: ConfirmCreateWalletFragmentArgs by navArgs()
    private var firstWord: Int = 0
    private var secondWord: Int = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentConfirmCreateWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firstWord = Random.nextInt(1, MNEMONIC_WORDS_COUNT)
        while (secondWord == 0 || secondWord == firstWord)
            secondWord = Random.nextInt(1, MNEMONIC_WORDS_COUNT)

        binding.inputWord1.hint =
            getString(R.string.label_word_confirm_create_wallet, firstWord.toString())
        binding.inputWord2.hint =
            getString(R.string.label_word_confirm_create_wallet, secondWord.toString())

        binding.buttonNextStep.setOnClickListener { checkConfirmations() }
    }

    private fun checkConfirmations() {
        val split = args.mnemonic.split(" ")

        var hadErrors = false
        binding.inputWord1.error = null
        binding.inputWord2.error = null

        if (!binding.inputWord1.editText?.text.toString().equals(split.get(firstWord - 1), false)) {
            binding.inputWord1.error = getString(R.string.error_word_confirm_create_wallet)
            hadErrors = true
        }
        if (!binding.inputWord2.editText?.text.toString().equals(split.get(secondWord - 1), false)) {
            binding.inputWord2.error = getString(R.string.error_word_confirm_create_wallet)
            hadErrors = true
        }

        if (!binding.checkConfirmCreateWallet.isChecked) {
            hadErrors = true
        }

        if (!hadErrors) {
            NavHostFragment.findNavController(requireParentFragment())
                .navigateSafe(
                    ConfirmCreateWalletFragmentDirections.actionConfirmCreateWalletFragmentToSaveWalletFragmentDialog(
                        args.mnemonic
                    )
                )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}