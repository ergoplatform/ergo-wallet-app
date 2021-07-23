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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
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

        // firing up appkit for the first time needs some time on medium end devices, so do this on
        // background thread while showing infinite progress bar
        lifecycleScope.launch(Dispatchers.IO) {
            val publicErgoAddressFromMnemonic = getPublicErgoAddressFromMnemonic(args.mnemonic)
            withContext(Dispatchers.Main) {
                binding.publicAddress.text = publicErgoAddressFromMnemonic
                binding.cardViewContainer.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        }

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

        // setDeviceCredentialAllowed is deprecated, but needed for older SDK level
        @Suppress("DEPRECATION") val promptInfo = PromptInfo.Builder()
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
                    saveToDbAndNavigateToWallet(
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

    private fun saveToDbAndNavigateToWallet(encType: Int, secretStorage: ByteArray) {
        // TODO avoid using mnemonic here, store and use publicAddress directly
        // Reason why this is not done: mnemonic is stored in arguments to allow Android to
        // destroy and recreate this dialog without the loss of the mnemonic. Possible
        // workarounds not implemented because of their drawbacks:
        // - Storing it as a SecretString in a shared ViewModel for the wallet creation dialogs
        //   Drawback: The ViewModel is reset when the destruction of the dialog is done due to low
        //             memory, hence we could lose the mnemonic on low end devices
        // - Use a static variable to store the mnemonic in a SecretString
        //   Drawback: It is completely out of control when static variables get reset and the
        //             variable might leak into a process reusing the JVM

        val context = requireContext()
        val mnemonic = args.mnemonic
        GlobalScope.launch(Dispatchers.IO) {
            // make sure not to use dialog context within this block
            val publicAddress = getPublicErgoAddressFromMnemonic(mnemonic)
            suspendSaveToDb(context, publicAddress, encType, secretStorage)
        }
        NavHostFragment.findNavController(requireParentFragment())
            .navigateSafe(SaveWalletFragmentDialogDirections.actionSaveWalletFragmentDialogToNavigationWallet())
    }

    override fun onPasswordEntered(password: String?): String? {
        if (password == null || password.length < 8) {
            return getString(R.string.err_password)
        } else {
            saveToDbAndNavigateToWallet(
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

/**
 * Saves the wallet data to DB. This is placed outside the dialog object, because the dialog might get
 * destroyed while the method is running. This leads occasional crashes when dialog context is
 * used here
 */
private suspend fun suspendSaveToDb(
    context: Context,
    publicAddress: String,
    encType: Int,
    secretStorage: ByteArray
) {
    // check if the wallet already exists
    val walletDao = AppDatabase.getInstance(context).walletDao()
    val existingWallet = walletDao.loadWalletByAddress(publicAddress)

    if (existingWallet != null) {
        // update encType and secret storage
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
                context.getString(R.string.label_wallet_default),
                publicAddress,
                encType,
                secretStorage
            )
        walletDao.insertAll(walletConfig)
        NodeConnector.getInstance().invalidateCache()
    }
}