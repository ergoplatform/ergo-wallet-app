package org.ergoplatform.android.wallet

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import org.ergoplatform.android.databinding.FragmentRestoreWalletBinding
import org.ergoplatform.android.ui.*
import org.ergoplatform.uilogic.wallet.RestoreWalletUiLogic

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

        val uiLogic = AndroidRestoreWalletUiLogic(requireContext())

        binding.tvMnemonic.editText?.setOnEditorActionListener { _, _, _ ->
            uiLogic.doRestore()
            true
        }
        binding.tvMnemonic.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                uiLogic.userChangedMnemonic()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        binding.buttonRestore.setOnClickListener { uiLogic.doRestore() }

        binding.labelWordListHint.enableLinks()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.tvMnemonic.editText?.requestFocus()
        forceShowSoftKeyboard(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class AndroidRestoreWalletUiLogic(context: Context) :
        RestoreWalletUiLogic(AndroidStringProvider(context)) {

        override fun getEnteredMnemonic(): CharSequence? = binding.tvMnemonic.editText?.text
        override fun setErrorLabel(error: String?) {
            binding.tvMnemonic.error = error
        }

        override fun navigateToSaveWalletDialog(mnemonic: String) {
            NavHostFragment.findNavController(requireParentFragment())
                .navigateSafe(
                    RestoreWalletFragmentDialogDirections.actionRestoreWalletFragmentDialogToSaveWalletFragmentDialog(
                        mnemonic
                    )
                )
        }

        override fun hideForcedSoftKeyboard() {
            hideForcedSoftKeyboard(requireContext(), binding.tvMnemonic.editText!!)
        }
    }
}