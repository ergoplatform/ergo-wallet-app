package org.ergoplatform

import org.junit.Assert.*

import org.junit.Test

class SigningSecretsTest {

    @Test
    fun serializationTests() {
        val signingSecretsDeprecated = SigningSecrets("mnemonic", true)
        assertEquals(signingSecretsDeprecated, SigningSecrets.fromJson(signingSecretsDeprecated.toJson()))
        val signingSecretsPast1627 = SigningSecrets("mnemonic", false)
        // TODO BIP-32 fix assertEquals(signingSecretsPast1627, SigningSecrets.fromJson(signingSecretsPast1627.toJson()))

        val oldVersion = "{\"mnemonic\":\"mnemonic\"}"
        val signingSecretsBackwardsCompatibility = SigningSecrets.fromJson(oldVersion)
        assertEquals(signingSecretsDeprecated, signingSecretsBackwardsCompatibility)
    }
}