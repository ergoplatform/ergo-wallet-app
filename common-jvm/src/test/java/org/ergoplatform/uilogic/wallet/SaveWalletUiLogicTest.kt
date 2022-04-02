package org.ergoplatform.uilogic.wallet

import org.ergoplatform.appkit.SecretString
import org.ergoplatform.getPublicErgoAddressFromMnemonic
import org.ergoplatform.isErgoMainNet
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveWalletUiLogicTest {

    val safeMnemonic =
        SecretString.create("vast wave minute approve turn turn assault phrase ladder since initial reunion exhibit wolf horse")
    val diffMnemonic =
        SecretString.create("race relax argue hair sorry riot there spirit ready fetch food hedgehog hybrid mobile pretty")
    val fixedAddress = "9eYMpbGgBf42bCcnB2nG3wQdqPzpCCw5eB1YaWUUen9uCaW3wwm"
    val deprAddress = "9ewv8sxJ1jfr6j3WUSbGPMTVx3TZgcJKdnjKCbJWhiJp5U62uhP"

    @Test
    fun checkAlternativeAddress() {
        isErgoMainNet = true
        val safeFromCreate = SaveWalletUiLogic(safeMnemonic, false)
        safeFromCreate.apply {
            assertEquals(false, hasAlternativeAddress)
            assertEquals(false, signingSecrets.deprecatedDerivation)
        }

        val safeFromRestore = SaveWalletUiLogic(safeMnemonic, true)
        safeFromRestore.apply {
            assertEquals(false, hasAlternativeAddress)
            assertEquals(false, signingSecrets.deprecatedDerivation)
        }

        val diffFromCreate = SaveWalletUiLogic(diffMnemonic, false)
        diffFromCreate.apply {
            assertEquals(false, hasAlternativeAddress)
            assertEquals(fixedAddress, publicAddress)
            assertEquals(false, signingSecrets.deprecatedDerivation)
            assertEquals(fixedAddress, getPublicErgoAddressFromMnemonic(signingSecrets))
            switchAddress()
            assertEquals(fixedAddress, publicAddress)
            assertEquals(fixedAddress, getPublicErgoAddressFromMnemonic(signingSecrets))
        }

        val diffFromRestore = SaveWalletUiLogic(diffMnemonic, true)
        diffFromRestore.apply {
            assertEquals(true, hasAlternativeAddress)
            assertEquals(fixedAddress, publicAddress)
            assertEquals(fixedAddress, getPublicErgoAddressFromMnemonic(signingSecrets))
            assertEquals(false, signingSecrets.deprecatedDerivation)
            switchAddress()
            assertEquals(true, hasAlternativeAddress)
            assertEquals(deprAddress, publicAddress)
            assertEquals(deprAddress, getPublicErgoAddressFromMnemonic(signingSecrets))
            assertEquals(true, signingSecrets.deprecatedDerivation)
        }
    }
}