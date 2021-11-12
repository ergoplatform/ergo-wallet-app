package org.ergoplatform.ios.ui

import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.deserializeSecrets
import org.ergoplatform.persistance.ENC_TYPE_PASSWORD
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.uilogic.STRING_ERROR_PASSWORD_EMPTY
import org.ergoplatform.uilogic.STRING_ERROR_PASSWORD_WRONG
import org.robovm.apple.uikit.UIViewController

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
