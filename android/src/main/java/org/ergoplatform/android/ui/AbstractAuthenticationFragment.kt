package org.ergoplatform.android.ui

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ergoplatform.SigningSecrets
import org.ergoplatform.android.R
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.api.AndroidEncryptionManager
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.persistance.ENC_TYPE_DEVICE
import org.ergoplatform.persistance.ENC_TYPE_PASSWORD
import org.ergoplatform.persistance.WalletConfig
import java.util.*

/**
 * Class to use when authentication with Biometrics and password to access mnemonic is needed
 */
abstract class AbstractAuthenticationFragment : Fragment(), PasswordDialogCallback {
    abstract val authenticationWalletConfig: WalletConfig?

    /**
     * Start the authentication flow, biometric prompt or password input depending on wallet
     */
    internal open fun startAuthFlow() {
        if (authenticationWalletConfig?.encryptionType == ENC_TYPE_PASSWORD) {
            PasswordDialogFragment().show(
                this.childFragmentManager,
                null
            )
        } else if (authenticationWalletConfig?.encryptionType == ENC_TYPE_DEVICE) {
            showBiometricPrompt()
        }
    }

    internal open fun showBiometricPrompt() {
        // setDeviceCredentialAllowed is deprecated, but needed for older SDK level
        @Suppress("DEPRECATION") val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.title_authenticate))
            .setConfirmationRequired(true) // don't display immediately when face is recognized
            .setDeviceCredentialAllowed(true)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    proceedAuthFlowFromBiometrics()
                } catch (t: Throwable) {
                    showBiometricPromptError(t.message ?: t.javaClass.simpleName)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                showBiometricPromptError(errString.toString())
            }
        }

        BiometricPrompt(this, callback).authenticate(promptInfo)
    }

    private fun showBiometricPromptError(errorMessage: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.error_device_security, errorMessage))
            .setPositiveButton(R.string.zxing_button_ok, null)
            .show()
    }

    /**
     * Called after a successful biometric authentication. This method may throw errors which
     * are shown to the user as a security error
     */
    private fun proceedAuthFlowFromBiometrics() {
        // we don't handle exceptions here by intention: we throw them back to the caller which
        // will show a snackbar to give the user a hint what went wrong
        authenticationWalletConfig?.secretStorage?.let {
            val decryptData = AndroidEncryptionManager.decryptDataWithDeviceKey(it)
            val signingSecrets = SigningSecrets.fromBytes(decryptData!!)
            proceedFromAuthFlow(signingSecrets!!)
            // do not erase secrets here: we have async operations
        }
    }

    override fun onPasswordEntered(password: SecretString?): String? {
        password?.let {
            if (!proceedAuthFlowWithPassword(password)) {
                return getString(R.string.error_password_wrong)
            } else {
                return null
            }
        }
        return getString(R.string.error_password_empty)
    }

    /**
     * Called after password is entered. Password may be wrong, so
     * @return false to show a warning about a wrong password
     */
    private fun proceedAuthFlowWithPassword(password: SecretString): Boolean {
        authenticationWalletConfig?.secretStorage?.let {
            val secrets: SigningSecrets?
            var decryptData: ByteArray? = null
            try {
                decryptData = AesEncryptionManager.decryptData(password, it)
                secrets = SigningSecrets.fromBytes(decryptData!!)
            } catch (t: Throwable) {
                // Password wrong
                decryptData?.let {
                    Arrays.fill(decryptData, 0)
                }
                return false
            }

            if (secrets == null) {
                // deserialization error, corrupted db data
                return false
            }

            proceedFromAuthFlow(secrets)

            return true
        }

        return false
    }

    abstract fun proceedFromAuthFlow(secrets: SigningSecrets)
}