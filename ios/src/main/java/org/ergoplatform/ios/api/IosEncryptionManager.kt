package org.ergoplatform.ios.api

import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.api.PasswordGenerator
import org.ergoplatform.utils.LogUtils
import org.robovm.apple.localauthentication.LAContext

object IosEncryptionManager {
    private const val KEYCHAIN_SERVICE = "org.ergoplatform.ios"
    private const val KEYCHAIN_ACCOUNT = "ergoWalletKey"

    // we generate keys of length 28 - a length of more than 25 considered as very strong
    private const val KEY_LENGTH = 28

    fun encryptDataWithKeychain(data: ByteArray, context: LAContext): ByteArray {
        val key = loadOrGenerateKey(context)
        return AesEncryptionManager.encryptData(key, data)
    }

    fun decryptDataWithKeychain(encryptedData: ByteArray, context: LAContext): ByteArray {
        val key = loadOrGenerateKey(context)
        return AesEncryptionManager.decryptData(key, encryptedData)
    }

    private fun loadOrGenerateKey(context: LAContext): String {
        val existingKey = IosKeychainAccess.queryPassword(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT, context)

        return if (existingKey == null) {
            LogUtils.logDebug("IosEncryptionManager", "Generating new key")
            val newKey = PasswordGenerator.generatePassword(KEY_LENGTH)
            IosKeychainAccess.savePassword(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT, newKey)
            newKey
        } else {
            LogUtils.logDebug("IosEncryptionManager", "Key read from keychain")
            existingKey
        }
    }

    fun emptyKeychain() {
        LogUtils.logDebug("IosEncryptionManager", "Deleting saved key from keychain")
        IosKeychainAccess.deletePassword(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT)
    }
}