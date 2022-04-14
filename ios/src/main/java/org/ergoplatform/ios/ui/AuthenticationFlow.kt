package org.ergoplatform.ios.ui

import org.ergoplatform.SigningSecrets
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.ios.api.IosAuthentication
import org.ergoplatform.ios.api.IosEncryptionManager
import org.ergoplatform.persistance.ENC_TYPE_DEVICE
import org.ergoplatform.persistance.ENC_TYPE_PASSWORD
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.robovm.apple.localauthentication.LAContext
import org.robovm.apple.uikit.*

fun UIViewController.startAuthFlow(wallet: WalletConfig, callback: (mnemonic: SigningSecrets) -> Unit) {
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
        IosAuthentication.authenticate(texts.get(STRING_TITLE_AUTHENTICATE),
            object : IosAuthentication.IosAuthCallback {
                override fun onAuthenticationSucceeded(context: LAContext) {
                    try {
                        val decrypted = IosEncryptionManager.decryptDataWithKeychain(
                            wallet.secretStorage!!, context
                        )

                        val mnemonic = SigningSecrets.fromBytes(decrypted)!!

                        runOnMainThread { callback.invoke(mnemonic) }
                    } catch (t: Throwable) {
                        LogUtils.logDebug("KeychainEnc", "Error accessing secrets", t)
                        runOnMainThread {
                            val uac = buildSimpleAlertController(
                                texts.format(STRING_ERROR_DEVICE_SECURITY, ""),
                                t.message ?: "", texts
                            )
                            presentViewController(uac, true) {}
                        }
                    }
                }

                override fun onAuthenticationError(error: String) {
                    runOnMainThread {
                        presentViewController(
                            buildSimpleAlertController(
                                texts.format(STRING_ERROR_DEVICE_SECURITY, ""),
                                error, texts
                            ), true
                        ) {}
                    }
                }

                override fun onAuthenticationCancelled() {
                    // do nothing
                }

            })
    }
}

private fun proceedAuthFlowWithPassword(
    wallet: WalletConfig,
    password: String,
    callback: (mnemonic: SigningSecrets) -> Unit
): Boolean {
    wallet.secretStorage?.let {
        val mnemonic: SigningSecrets?
        try {
            val decryptData = AesEncryptionManager.decryptData(password, it)
            mnemonic = SigningSecrets.fromBytes(decryptData!!)
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
