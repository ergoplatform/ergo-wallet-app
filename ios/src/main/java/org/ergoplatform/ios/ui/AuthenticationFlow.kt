package org.ergoplatform.ios.ui

import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.deserializeSecrets
import org.ergoplatform.ios.api.IosAuthentication
import org.ergoplatform.ios.api.IosEncryptionManager
import org.ergoplatform.persistance.ENC_TYPE_DEVICE
import org.ergoplatform.persistance.ENC_TYPE_PASSWORD
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.robovm.apple.localauthentication.LAContext
import org.robovm.apple.uikit.*

fun UIViewController.startAuthFlow(wallet: WalletConfig, callback: (mnemonic: String) -> Unit) {
    val texts = getAppDelegate().texts
    if (wallet.encryptionType == ENC_TYPE_PASSWORD) {
        PasswordViewController.showDialog(this, fun(pw): String? {
            pw?.let {
                if (!proceedAuthFlowWithPassword(wallet, pw, callback)) {
                    return texts.get(STRING_ERROR_PASSWORD_WRONG)
                } else {
                    return null
                }
            }
            return texts.get(STRING_ERROR_PASSWORD_EMPTY)
        })
    } else if (wallet.encryptionType == ENC_TYPE_DEVICE) {
        // TODO Biometrics test on device when auth is disabled after saving the wallet
        IosAuthentication.authenticate(texts.get(STRING_TITLE_AUTHENTICATE),
            object : IosAuthentication.IosAuthCallback {
                override fun onAuthenticationSucceeded(context: LAContext) {
                    try {
                        val decrypted = IosEncryptionManager.decryptDataWithKeychain(
                            wallet.secretStorage!!
                        )

                        val mnemonic = deserializeSecrets(String(decrypted))!!

                        runOnMainThread { callback.invoke(mnemonic) }
                    } catch (t: Throwable) {
                        LogUtils.logDebug("KeychainEnc", "Error accessing secrets", t)
                        runOnMainThread {
                            val uac = UIAlertController(
                                texts.format(STRING_ERROR_DEVICE_SECURITY, ""),
                                t.message, UIAlertControllerStyle.Alert
                            )
                            uac.addAction(
                                UIAlertAction(
                                    texts.get(STRING_ZXING_BUTTON_OK),
                                    UIAlertActionStyle.Default
                                ) {})
                            presentViewController(uac, true) {}
                        }
                    }
                }

                override fun onAuthenticationError(error: String) {
                    // do nothing
                }

            })
    }
}

private fun proceedAuthFlowWithPassword(
    wallet: WalletConfig,
    password: String,
    callback: (mnemonic: String) -> Unit
): Boolean {
    wallet.secretStorage?.let {
        val mnemonic: String?
        try {
            val decryptData = AesEncryptionManager.decryptData(password, it)
            mnemonic = deserializeSecrets(String(decryptData!!))
        } catch (t: Throwable) {
            // Password wrong
            return false
        }

        if (mnemonic == null) {
            // deserialization error, corrupted db data
            return false
        }

        callback.invoke(mnemonic)
        return true
    }

    return false
}
