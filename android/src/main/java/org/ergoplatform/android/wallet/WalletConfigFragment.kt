package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.*
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentWalletConfigBinding
import org.ergoplatform.android.ui.*
import org.ergoplatform.getSerializedXpubKeyFromMnemonic

/**
 * Shows settings and details for a wallet
 */
class WalletConfigFragment : AbstractAuthenticationFragment(), ConfirmationCallback {

    var _binding: FragmentWalletConfigBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: WalletConfigViewModel

    private val args: WalletConfigFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this).get(WalletConfigViewModel::class.java)

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

                binding.buttonAddresses.setOnClickListener {
                    findNavController().navigateSafe(
                        WalletConfigFragmentDirections.actionWalletConfigFragmentToWalletAddressesFragment(
                            wallet.id
                        )
                    )
                }

                binding.buttonExport.isEnabled = it.secretStorage != null
                binding.buttonDisplayXpubkey.isEnabled =
                    it.extendedPublicKey != null || it.secretStorage != null
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

        binding.buttonExport.setOnClickListener {
            viewModel.uiLogic.wallet?.let {
                viewModel.mnemonicNeededFor =
                    WalletConfigViewModel.MnemonicNeededFor.DISPLAY_MNEMONIC
                startAuthFlow(it)
            }
        }

        binding.buttonDisplayXpubkey.setOnClickListener {
            viewModel.uiLogic.wallet?.secretStorage?.let {
                viewModel.mnemonicNeededFor = WalletConfigViewModel.MnemonicNeededFor.SHOW_XPUB
                startAuthFlow(viewModel.uiLogic.wallet!!)
            } ?: viewModel.uiLogic.wallet?.extendedPublicKey?.let {
                displayXpubKey(it)
            }
        }

        viewModel.snackbarEvent.observe(
            viewLifecycleOwner,
            {
                Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.nav_view).show()
            }
        )
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

    override fun proceedAuthFlowWithPassword(password: String): Boolean {
        val mnemonic = viewModel.decryptMnemonicWithPass(password)
        if (mnemonic == null) {
            return false
        } else if (viewModel.mnemonicNeededFor == WalletConfigViewModel.MnemonicNeededFor.DISPLAY_MNEMONIC) {
            displayMnemonic(mnemonic)
        } else {
            displayXpubKeyFromMnemonic(mnemonic)
        }
        return true
    }

    override fun proceedAuthFlowFromBiometrics() {
        val mnemonic = viewModel.decryptMnemonicWithUserAuth()!!
        if (viewModel.mnemonicNeededFor == WalletConfigViewModel.MnemonicNeededFor.DISPLAY_MNEMONIC) {
            displayMnemonic(mnemonic)
        } else {
            displayXpubKeyFromMnemonic(mnemonic)
        }
    }

    private fun displayXpubKeyFromMnemonic(mnemonic: String) {
        displayXpubKey(getSerializedXpubKeyFromMnemonic(mnemonic))
    }

    private fun displayXpubKey(xpubkey: String) {
        findNavController().navigateSafe(
            WalletConfigFragmentDirections.actionWalletConfigFragmentToShareWithQrDialogFragment(
                xpubkey
            )
        )
    }

    private fun displayMnemonic(mnemonic: String) {
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