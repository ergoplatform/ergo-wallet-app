package org.ergoplatform.ios.api

import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.utils.LogUtils
import kotlin.random.Random

object IosEncryptionManager {
    private const val KEYCHAIN_SERVICE = "org.ergoplatform.ios"
    private const val KEYCHAIN_ACCOUNT = "ergoWalletKey"
    private const val KEY_LENGTH = 28

    fun encryptDataWithKeychain(data: ByteArray): ByteArray {
        val key = loadOrGenerateKey()
        return AesEncryptionManager.encryptData(key, data)
    }

    fun decryptDataWithKeychain(encryptedData: ByteArray): ByteArray {
        val key = loadOrGenerateKey()
        return AesEncryptionManager.decryptData(key, encryptedData)
    }

    private fun getRandomKey(): String {
        val chars = ('a'..'Z') + ('A'..'Z') + ('0'..'9')
        val rnd = Random(System.nanoTime())
        return List(KEY_LENGTH) { chars.random(rnd) }.joinToString("")
    }

    private fun loadOrGenerateKey(): String {
        val existingKey = IosKeychainAccess.queryPassword(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT)

        return if (existingKey == null) {
            LogUtils.logDebug("IosEncryptionManager", "Generating new key")
            val newKey = getRandomKey()
            IosKeychainAccess.savePassword(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT, newKey)
            newKey
        } else
            existingKey
    }

    fun emptyKeychain() {
        LogUtils.logDebug("IosEncryptionManager", "Deleting saved key from keychain")
        IosKeychainAccess.deletePassword(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT)
    }
}