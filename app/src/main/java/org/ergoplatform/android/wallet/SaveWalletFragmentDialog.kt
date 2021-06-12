package org.ergoplatform.android.wallet

import StageConstants
import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.NodeConnector
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentSaveWalletDialogBinding
import org.ergoplatform.android.serializeSecrets
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.PasswordDialogCallback
import org.ergoplatform.android.ui.PasswordDialogFragment
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.SecretString

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

        val fromMnemonic = Address.fromMnemonic(
            StageConstants.NETWORK_TYPE,
            SecretString.create(args.mnemonic),
            SecretString.create("")
        )

        binding.publicAddress.text = fromMnemonic.ergoAddress.toString()

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

        binding.buttonSavePassenc.setOnClickListener {
            PasswordDialogFragment().show(
                childFragmentManager,
                null
            )
        }
        binding.buttonSaveDeviceenc.setOnClickListener { TODO() }
    }

    private fun saveToDb(encType: Int, secretStorage: ByteArray) {
        val fromMnemonic = Address.fromMnemonic(
            StageConstants.NETWORK_TYPE,
            SecretString.create(args.mnemonic),
            SecretString.create("")
        )

        val walletConfig =
            WalletConfigDbEntity(
                0,
                "My wallet",
                fromMnemonic.ergoAddress.toString(),
                encType,
                secretStorage
            )

        GlobalScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(requireContext()).walletDao().insertAll(walletConfig)
            NodeConnector.getInstance().invalidateCache()
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