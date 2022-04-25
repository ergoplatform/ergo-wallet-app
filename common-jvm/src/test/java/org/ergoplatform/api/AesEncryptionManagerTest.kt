package org.ergoplatform.api

import org.ergoplatform.appkit.SecretString
import org.junit.Assert
import org.junit.Test

class AesEncryptionManagerTest {

    @Test
    fun encryptAndDecryptData() {
        // check decryption of string made on iOS with isOnLegacyApi = true
        val list = listOf(
            0, 0, 0, 12, -124, -71, 94, 114, 18, -85, 124, 97, -74, -43, 121, -89, -11, -39, -37, 22, -18, 29, 125,
            13, 78, -84, 60, -58, -128, -42, 46, -121, -111, -80, -49, 55, 52, -1, 9, -104, 26, 92, -57, 116, -43,
            -51, -117, 79, 7, -46, 32, -78, 58, 123, 57, 2, 60, -76, -36, -123, 77, -82, 71, 41, -53, -40, -100, 7,
            -24, -108, -100, 48, 28
        )
        Assert.assertEquals(
            "{\"mnemonic\":\"1 2 3 4 5 6 7 8 9 10 11 12\"}",
            String(AesEncryptionManager.decryptData(SecretString.create("passwort"), ByteArray(list.size, { list[it].toByte() })))
        )

        val secretData = "SecretData"
        val password = SecretString.create("password")
        val encryptedData = AesEncryptionManager.encryptData(password, secretData.toByteArray())

        Assert.assertEquals(secretData, String(AesEncryptionManager.decryptData(password, encryptedData)))

        Assert.assertThrows(
            Throwable::class.java
        ) { AesEncryptionManager.decryptData(SecretString.create("wrong"), encryptedData) }
    }
}