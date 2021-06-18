package org.ergoplatform.android.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentWalletConfigBinding
import org.ergoplatform.android.ui.*

/**
 * Shows settings and details for a wallet
 */
class WalletConfigFragment : Fragment(), ConfirmationCallback, PasswordDialogCallback {

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

        lifecycleScope.launch {
            val wallet =
                AppDatabase.getInstance(requireContext()).walletDao().loadWalletById(args.walletId)

            wallet?.let {
                binding.publicAddress.text = wallet.publicAddress
                binding.inputWalletName.editText?.setText(wallet.displayName)

                binding.buttonCopy.setOnClickListener {
                    val clipboard = ContextCompat.getSystemService(
                        requireContext(),
                        ClipboardManager::class.java
                    )
                    val clip = ClipData.newPlainText("", wallet.publicAddress)
                    clipboard?.setPrimaryClip(clip)

                    Snackbar.make(requireView(), R.string.label_copied, Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.nav_view).show()
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
                args.walletId,
                binding.inputWalletName.editText?.text?.toString()
            )
        }

        binding.buttonExport.setOnClickListener {
            viewModel.prepareDisplayMnemonic(this, args.walletId)
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

    override fun onPasswordEntered(password: String?): String? {
        password?.let {
            val mnemonic = viewModel.decryptMnemonicWithPass(password)
            if (mnemonic == null) {
                return getString(R.string.error_password_wrong)
            } else {
                displayMnemonic(mnemonic)
                return null
            }
        }
        return getString(R.string.error_password_empty)
    }

    fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.title_authenticate))
            .setConfirmationRequired(true) // don't display immediately when face is recognized
            .setDeviceCredentialAllowed(true)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val mnemonic = viewModel.decryptMnemonicWithUserAuth()
                    displayMnemonic(mnemonic!!)
                } catch (t: Throwable) {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.error_device_security, t.message),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Snackbar.make(
                    requireView(),
                    getString(R.string.error_device_security, errString),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        BiometricPrompt(this, callback).authenticate(promptInfo)
    }

    private fun displayMnemonic(mnemonic: String) {
        MaterialAlertDialogBuilder(requireContext()).setMessage(mnemonic)
            .setPositiveButton(R.string.button_done, null).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}