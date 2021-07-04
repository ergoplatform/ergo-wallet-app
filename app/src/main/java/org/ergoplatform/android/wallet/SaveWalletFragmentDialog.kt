package org.ergoplatform.android.wallet

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.*
import org.ergoplatform.android.databinding.FragmentSaveWalletDialogBinding
import org.ergoplatform.android.ui.*
import org.ergoplatform.api.AesEncryptionManager

/**
 * Dialog to save a created or restored wallet
 */
class SaveWalletFragmentDialog : FullScreenFragmentDialog(), PasswordDialogCallback {
    private var _binding: FragmentSaveWalletDialogBinding? = null
    private val binding get() = _binding!!

    private val args: SaveWalletFragmentDialogArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSaveWalletDialogBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.publicAddress.text = getPublicErgoAddressFromMnemonic(args.mnemonic)

        val bmm = BiometricManager.from(requireContext())
        val methodDesc =
            if (bmm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS)
                R.string.device_enc_security_biometric_strong
            else if (bmm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS)
                R.string.device_enc_security_biometric_weak
            else if ((requireContext().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure)
                R.string.device_enc_security_pass
            else R.string.device_enc_security_none

        binding.descDeviceEncryption.text =
            getString(R.string.desc_save_device_encrypted, getString(methodDesc))

        if (methodDesc == R.string.device_enc_security_none) {
            binding.buttonSaveDeviceenc.isEnabled = false
        }

        binding.buttonSavePassenc.setOnClickListener {
            val passwordDialogFragment = PasswordDialogFragment()
            val args = Bundle()
            args.putBoolean(ARG_SHOW_CONFIRMATION, true)
            passwordDialogFragment.arguments = args
            passwordDialogFragment.show(
                childFragmentManager,
                null
            )
        }
        binding.buttonSaveDeviceenc.setOnClickListener {
            showBiometricPrompt()
        }
    }

    fun showBiometricPrompt() {

        val promptInfo = PromptInfo.Builder()
            .setTitle(getString(R.string.title_authenticate))
            .setConfirmationRequired(false)
            .setDeviceCredentialAllowed(true)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val secretStorage = AesEncryptionManager.encryptDataOnDevice(
                        serializeSecrets(args.mnemonic).toByteArray()
                    )
                    saveToDb(
                        ENC_TYPE_DEVICE, secretStorage
                    )

                } catch (t: Throwable) {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.error_device_security, t.message),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        BiometricPrompt(this, callback).authenticate(promptInfo)
    }

    private fun saveToDb(encType: Int, secretStorage: ByteArray) {
        val publicAddress = getPublicErgoAddressFromMnemonic(args.mnemonic)

        GlobalScope.launch(Dispatchers.IO) {
            // check if the wallet already exists
            val walletDao = AppDatabase.getInstance(requireContext()).walletDao()
            val existingWallet = walletDao.loadWalletByAddress(publicAddress)

            if (existingWallet != null) {
                // update enctype and secret storage
                val walletConfig = WalletConfigDbEntity(
                    existingWallet.id,
                    existingWallet.displayName,
                    existingWallet.publicAddress,
                    encType,
                    secretStorage
                )
                walletDao.update(walletConfig)
            } else {
                val walletConfig =
                    WalletConfigDbEntity(
                        0,
                        getString(R.string.label_wallet_default),
                        publicAddress,
                        encType,
                        secretStorage
                    )
                walletDao.insertAll(walletConfig)
                NodeConnector.getInstance().invalidateCache()
            }
        }
        NavHostFragment.findNavController(requireParentFragment())
            .navigateSafe(SaveWalletFragmentDialogDirections.actionSaveWalletFragmentDialogToNavigationWallet())
    }

    override fun onPasswordEntered(password: String?): String? {
        if (password == null || password.length < 8) {
            return getString(R.string.err_password)
        } else {
            saveToDb(
                ENC_TYPE_PASSWORD,
                AesEncryptionManager.encryptData(
                    password,
                    serializeSecrets(args.mnemonic).toByteArray()
                )
            )
            return null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}