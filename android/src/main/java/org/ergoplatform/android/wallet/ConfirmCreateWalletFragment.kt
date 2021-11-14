package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentConfirmCreateWalletBinding
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.uilogic.wallet.ConfirmCreateWalletUiLogic

/**
 * Create new wallet, step 2:
 * User confirms creation of wallet by entering two mnemonic words and some checkboxes
 */
class ConfirmCreateWalletFragment : FullScreenFragmentDialog() {

    private var _binding: FragmentConfirmCreateWalletBinding? = null
    private val binding get() = _binding!!

    private val args: ConfirmCreateWalletFragmentArgs by navArgs()
    private val uiLogic = ConfirmCreateWalletUiLogic()

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

        uiLogic.mnemonic = SecretString.create(args.mnemonic)
        binding.inputWord1.hint =
            getString(R.string.label_word_confirm_create_wallet, uiLogic.firstWord.toString())
        binding.inputWord2.hint =
            getString(R.string.label_word_confirm_create_wallet, uiLogic.secondWord.toString())

        binding.buttonNextStep.setOnClickListener { checkConfirmations() }
    }

    private fun checkConfirmations() {

        val hadErrors = uiLogic.checkConfirmations(
            binding.inputWord1.editText?.text.toString(),
            binding.inputWord2.editText?.text.toString(), binding.checkConfirmCreateWallet.isChecked
        )

        binding.inputWord1.error =
            if (uiLogic.firstWordCorrect) null else getString(R.string.error_word_confirm_create_wallet)
        binding.inputWord2.error =
            if (uiLogic.secondWordCorrect) null else getString(R.string.error_word_confirm_create_wallet)

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