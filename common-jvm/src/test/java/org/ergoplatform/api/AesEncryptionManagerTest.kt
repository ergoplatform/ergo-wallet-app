package org.ergoplatform.api

import org.junit.Assert
import org.junit.Test

class AesEncryptionManagerTest {

    @Test
    fun encryptAndDecryptData() {
        val secretData = "SecretData".encodeToByteArray()
        val password = "password"
        val encryptedData = AesEncryptionManager.encryptData(password, secretData)

        Assert.assertEquals(String(secretData), String(AesEncryptionManager.decryptData(password, encryptedData)))

        Assert.assertThrows(
            Throwable::class.java
        ) { AesEncryptionManager.decryptData("wrong", encryptedData) }
    }
}