package org.ergoplatform.android.ui

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.ergoplatform.android.R
import org.ergoplatform.android.wallet.ENC_TYPE_DEVICE
import org.ergoplatform.android.wallet.ENC_TYPE_PASSWORD
import org.ergoplatform.android.wallet.WalletConfigDbEntity

/**
 * Class to use when authentication with Biometrics and password to access mnemonic is needed
 */
abstract class AbstractAuthenticationFragment : Fragment(), PasswordDialogCallback {

    /**
     * Start the authentication flow, biometric prompt or password input depending on wallet
     */
    internal fun startAuthFlow(walletConfig: WalletConfigDbEntity) {
        if (walletConfig.encryptionType == ENC_TYPE_PASSWORD) {
            PasswordDialogFragment().show(
                this.childFragmentManager,
                null
            )
        } else if (walletConfig.encryptionType == ENC_TYPE_DEVICE) {
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
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
                    view?.let {
                        Snackbar.make(
                            it,
                            getString(R.string.error_device_security, t.message),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                view?.let {
                    Snackbar.make(
                        it,
                        getString(R.string.error_device_security, errString),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        BiometricPrompt(this, callback).authenticate(promptInfo)
    }

    /**
     * Called after a successful biometric authentication. This method may throw errors which
     * are shown to the user as a security error
     */
    abstract fun proceedAuthFlowFromBiometrics()

    override fun onPasswordEntered(password: String?): String? {
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
    abstract fun proceedAuthFlowWithPassword(password: String): Boolean
}