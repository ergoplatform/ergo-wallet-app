package org.ergoplatform

import org.junit.Assert.*

import org.junit.Test

class SigningSecretsTest {

    @Test
    fun serializationTests() {
        val signingSecretsDeprecated = SigningSecrets("mnemonic✅", true)
        assertEquals(signingSecretsDeprecated, SigningSecrets.fromBytes(signingSecretsDeprecated.toBytes()))
        val signingSecretsPast1627 = SigningSecrets("mnemonic", false)
        // TODO BIP-32 fix assertEquals(signingSecretsPast1627, SigningSecrets.fromBytes(signingSecretsPast1627.toBytes()))

        val oldSerializationVersion = "{\"mnemonic\":\"mnemonic✅\"}".toByteArray()
        val signingSecretsBackwardsCompatibility = SigningSecrets.fromBytes(oldSerializationVersion)
        assertEquals(signingSecretsDeprecated, signingSecretsBackwardsCompatibility)

        val newSerializationVersion = "mnemonic✅".toByteArray()
        val signingSecrets = SigningSecrets.fromBytes(newSerializationVersion)
        assertEquals(signingSecretsDeprecated, signingSecrets)
    }
}