package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.*
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.ergoplatform.SigningSecrets
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentWalletConfigBinding
import org.ergoplatform.android.ui.*
import org.ergoplatform.compose.wallet.MultisigInfoSection
import org.ergoplatform.compose.wallet.WalletInfoSection
import org.ergoplatform.getSerializedXpubKeyFromMnemonic
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.wallet.isMultisig

/**
 * Shows settings and details for a wallet
 */
class WalletConfigFragment : AbstractAuthenticationFragment(), ConfirmationCallback {

    var _binding: FragmentWalletConfigBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WalletConfigViewModel by viewModels()

    private val args: WalletConfigFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentWalletConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.init(args.walletId, requireContext())

        viewModel.walletConfig.observe(viewLifecycleOwner) { wallet ->
            wallet?.let {
                binding.publicAddress.text = wallet.firstAddress
                binding.inputWalletName.editText?.setText(wallet.displayName)

                binding.buttonCopy.setOnClickListener {
                    copyStringToClipboard(wallet.firstAddress!!, requireContext(), requireView())
                }

                binding.composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                binding.composeView.setContent {
                    AppComposeTheme {
                        Column {
                            if (wallet.isMultisig())
                                MultisigInfoSection(
                                    viewModel.uiLogic.multisigInfoFlow,
                                    AndroidStringProvider(requireContext()),
                                )
                            else
                                WalletInfoSection(
                                    onAddAddresses = {
                                        findNavController().navigateSafe(
                                            WalletConfigFragmentDirections.actionWalletConfigFragmentToWalletAddressesFragment(
                                                wallet.id
                                            )
                                        )
                                    },
                                    ::displayXpubkey,
                                    wallet,
                                    ::onShowMnemonic,
                                    AndroidStringProvider(requireContext()),
                                )
                        }
                    }
                }
            }
        }

        binding.inputWalletName.editText?.setOnEditorActionListener { _, _, _ ->
            binding.buttonApply.callOnClick()
            true
        }

        binding.buttonApply.setOnClickListener {
            hideForcedSoftKeyboard(requireContext(), binding.inputWalletName.editText!!)
            viewModel.saveChanges(
                requireContext(),
                binding.inputWalletName.editText?.text?.toString()
            )
        }

        viewModel.snackbarEvent.observe(viewLifecycleOwner) {
            Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.nav_view).show()
        }
    }

    private fun onShowMnemonic() {
        viewModel.uiLogic.wallet?.let {
            viewModel.mnemonicNeededFor =
                WalletConfigViewModel.MnemonicNeededFor.DISPLAY_MNEMONIC
            startAuthFlow()
        }
    }

    private fun displayXpubkey() {
        viewModel.uiLogic.wallet?.secretStorage?.let {
            viewModel.mnemonicNeededFor = WalletConfigViewModel.MnemonicNeededFor.SHOW_XPUB
            startAuthFlow()
        } ?: viewModel.uiLogic.wallet?.extendedPublicKey?.let {
            displayXpubKey(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_wallet_config, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_delete) {
            val confirmationDialogFragment = ConfirmationDialogFragment()
            val args = Bundle()
            args.putString(ARG_CONFIRMATION_TEXT, getString(R.string.label_confirm_delete))
            args.putString(ARG_BUTTON_YES_LABEL, getString(R.string.button_delete))
            confirmationDialogFragment.arguments = args
            confirmationDialogFragment.show(childFragmentManager, null)

            return true
        } else
            return super.onOptionsItemSelected(item)
    }

    override fun onConfirm() {
        // deletion was confirmed
        viewModel.deleteWallet(requireContext(), args.walletId)
        findNavController().navigateUp()
    }

    override val authenticationWalletConfig: WalletConfig?
        get() = viewModel.uiLogic.wallet

    override fun proceedFromAuthFlow(secrets: SigningSecrets) {
        if (viewModel.mnemonicNeededFor == WalletConfigViewModel.MnemonicNeededFor.DISPLAY_MNEMONIC) {
            displayMnemonic(secrets)
        } else {
            displayXpubKeyFromMnemonic(secrets)
        }
    }

    private fun displayXpubKeyFromMnemonic(signingSecrets: SigningSecrets) {
        val xpubkey = getSerializedXpubKeyFromMnemonic(signingSecrets)
        signingSecrets.clearMemory()
        displayXpubKey(xpubkey)
    }

    private fun displayXpubKey(xpubkey: String) {
        findNavController().navigateSafe(
            WalletConfigFragmentDirections.actionWalletConfigFragmentToShareWithQrDialogFragment(
                xpubkey
            )
        )
    }

    private fun displayMnemonic(signingSecrets: SigningSecrets) {
        val mnemonic = signingSecrets.mnemonic.toStringUnsecure()
        signingSecrets.clearMemory()

        MaterialAlertDialogBuilder(requireContext())
            .setMessage(mnemonic)
            .setPositiveButton(R.string.button_copy) { _, _ ->
                showSensitiveDataCopyDialog(requireContext(), mnemonic)
            }
            .setNegativeButton(R.string.label_dismiss, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}