package org.ergoplatform.uilogic.ergoauth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.ergoplatform.SigningSecrets
import org.ergoplatform.TestStringProvider
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.SigmaProp
import org.ergoplatform.ergoauth.ErgoAuthRequest
import org.ergoplatform.ergoauth.ErgoAuthResponse
import org.ergoplatform.getPublicErgoAddressFromMnemonic
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.mosaik.MosaikDbProvider
import org.ergoplatform.persistance.AddressBookDbProvider
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.TokenDbProvider
import org.ergoplatform.persistance.TransactionDbProvider
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.transactions.QR_DATA_LENGTH_LIMIT
import org.ergoplatform.transactions.ergoAuthRequestToQrChunks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ErgoAuthUiLogicTest {

    private val firstMnemonic =
        "slow silly start wash bundle suffer bulb ancient height spin express remind today effort helmet"
    private val secondMnemonic =
        "vast wave minute approve turn turn assault phrase ladder since initial reunion exhibit wolf horse"

    @Test
    fun testSwitchWalletReloadsDerivedAddressesForSigning() {
        runBlocking {
            val prevMainNet = isErgoMainNet
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                isErgoMainNet = false

                val wallet1Secrets = SigningSecrets(firstMnemonic, false)
                val wallet2Secrets = SigningSecrets(secondMnemonic, false)
                val wallet1Addr0 = getPublicErgoAddressFromMnemonic(wallet1Secrets, 0)
                val wallet2Addr0 = getPublicErgoAddressFromMnemonic(wallet2Secrets, 0)
                val wallet2Addr1 = getPublicErgoAddressFromMnemonic(wallet2Secrets, 1)
                val wallet2Addr2 = getPublicErgoAddressFromMnemonic(wallet2Secrets, 2)

                val wallet1Config = WalletConfig(1, "Wallet 1", wallet1Addr0, 0, null, false, null)
                val wallet2Config = WalletConfig(2, "Wallet 2", wallet2Addr0, 0, null, false, null)

                val walletDbProvider = mock<WalletDbProvider> {}
                whenever(walletDbProvider.loadWalletConfigById(1)).thenReturn(wallet1Config)
                whenever(walletDbProvider.loadWalletConfigById(2)).thenReturn(wallet2Config)
                whenever(walletDbProvider.loadWalletAddresses(wallet1Addr0)).thenReturn(emptyList())
                whenever(walletDbProvider.loadWalletAddresses(wallet2Addr0)).thenReturn(
                    listOf(
                        WalletAddress(1L, wallet2Addr0, 1, wallet2Addr1, null),
                        WalletAddress(2L, wallet2Addr0, 2, wallet2Addr2, null),
                    )
                )
                val db = buildTestDatabase(walletDbProvider)

                val authRequest = ErgoAuthRequest(
                    signingMessage = "test-auth-message",
                    sigmaBoolean = SigmaProp(Address.create(wallet2Addr2).publicKey),
                    userMessage = "Authenticate",
                    messageSeverity = MessageSeverity.INFORMATION,
                    replyToUrl = null,
                    requestHost = "",
                    sslValidatedBy = null,
                )
                val qrChunk =
                    ergoAuthRequestToQrChunks(authRequest.toColdAuthRequest(), QR_DATA_LENGTH_LIMIT)
                        .first()

                val uiLogic = TestErgoAuthUiLogic(scope)
                uiLogic.init(qrChunk, wallet1Config.id, TestStringProvider(), db)
                waitForState(uiLogic, ErgoAuthUiLogic.State.WAIT_FOR_AUTH)

                assertEquals(ErgoAuthUiLogic.State.WAIT_FOR_AUTH, uiLogic.state)
                assertEquals(wallet1Config, uiLogic.walletConfig)
                assertEquals(listOf(0), uiLogic.walletAddresses.map { it.derivationIndex })

                uiLogic.setWalletConfig(wallet2Config, db.walletDbProvider)
                assertEquals(wallet2Config, uiLogic.walletConfig)

                uiLogic.startResponse(SigningSecrets(secondMnemonic, false), TestStringProvider())
                waitForState(uiLogic, ErgoAuthUiLogic.State.DONE)

                assertEquals(ErgoAuthUiLogic.State.DONE, uiLogic.state)
                assertEquals(listOf(0, 1, 2), uiLogic.walletAddresses.map { it.derivationIndex })
                assertEquals(MessageSeverity.INFORMATION, uiLogic.getDoneSeverity())
                assertNull(uiLogic.lastMessage)
                assertNotNull(uiLogic.coldSerializedAuthResponse)

                val parsedResponse = ErgoAuthResponse.fromJson(uiLogic.coldSerializedAuthResponse!!)
                assertTrue(parsedResponse.proof.isNotEmpty())
                assertTrue(parsedResponse.signedMessage.contains("test-auth-message"))
            } finally {
                scope.cancel()
                isErgoMainNet = prevMainNet
            }
        }
    }

    @Test
    fun testSwitchWalletReplacesPreviousWalletDerivedIndices() {
        runBlocking {
            val prevMainNet = isErgoMainNet
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                isErgoMainNet = false

                val wallet1Secrets = SigningSecrets(firstMnemonic, false)
                val wallet2Secrets = SigningSecrets(secondMnemonic, false)
                val wallet1Addr0 = getPublicErgoAddressFromMnemonic(wallet1Secrets, 0)
                val wallet1Addr1 = getPublicErgoAddressFromMnemonic(wallet1Secrets, 1)
                val wallet2Addr0 = getPublicErgoAddressFromMnemonic(wallet2Secrets, 0)
                val wallet2Addr1 = getPublicErgoAddressFromMnemonic(wallet2Secrets, 1)

                val wallet1Config = WalletConfig(1, "Wallet 1", wallet1Addr0, 0, null, false, null)
                val wallet2Config = WalletConfig(2, "Wallet 2", wallet2Addr0, 0, null, false, null)

                val walletDbProvider = mock<WalletDbProvider> {}
                whenever(walletDbProvider.loadWalletConfigById(1)).thenReturn(wallet1Config)
                whenever(walletDbProvider.loadWalletConfigById(2)).thenReturn(wallet2Config)
                whenever(walletDbProvider.loadWalletAddresses(wallet1Addr0)).thenReturn(emptyList())
                whenever(walletDbProvider.loadWalletAddresses(wallet2Addr0)).thenReturn(
                    listOf(WalletAddress(1L, wallet2Addr0, 1, wallet2Addr1, null))
                )
                val db = buildTestDatabase(walletDbProvider)

                val authRequest = ErgoAuthRequest(
                    signingMessage = "test-auth-message",
                    sigmaBoolean = SigmaProp(Address.create(wallet1Addr1).publicKey),
                    userMessage = "Authenticate",
                    messageSeverity = MessageSeverity.INFORMATION,
                    replyToUrl = null,
                    requestHost = "",
                    sslValidatedBy = null,
                )
                val qrChunk =
                    ergoAuthRequestToQrChunks(authRequest.toColdAuthRequest(), QR_DATA_LENGTH_LIMIT)
                        .first()

                val uiLogic = TestErgoAuthUiLogic(scope)
                uiLogic.init(qrChunk, wallet2Config.id, TestStringProvider(), db)
                waitForState(uiLogic, ErgoAuthUiLogic.State.WAIT_FOR_AUTH)

                assertEquals(ErgoAuthUiLogic.State.WAIT_FOR_AUTH, uiLogic.state)
                assertEquals(listOf(0, 1), uiLogic.walletAddresses.map { it.derivationIndex })

                uiLogic.setWalletConfig(wallet1Config, db.walletDbProvider)
                assertEquals(wallet1Config, uiLogic.walletConfig)

                uiLogic.startResponse(SigningSecrets(firstMnemonic, false), TestStringProvider())
                waitForState(uiLogic, ErgoAuthUiLogic.State.DONE)

                assertEquals(ErgoAuthUiLogic.State.DONE, uiLogic.state)
                assertEquals(listOf(0), uiLogic.walletAddresses.map { it.derivationIndex })
                assertEquals(MessageSeverity.ERROR, uiLogic.getDoneSeverity())
                assertNull(uiLogic.coldSerializedAuthResponse)
            } finally {
                scope.cancel()
                isErgoMainNet = prevMainNet
            }
        }
    }

    private fun buildTestDatabase(walletDbProvider: WalletDbProvider): IAppDatabase {
        return object : IAppDatabase {
            override val walletDbProvider: WalletDbProvider
                get() = walletDbProvider
            override val tokenDbProvider: TokenDbProvider
                get() = mock {}
            override val transactionDbProvider: TransactionDbProvider
                get() = mock {}
            override val mosaikDbProvider: MosaikDbProvider
                get() = mock {}
            override val addressBookDbProvider: AddressBookDbProvider
                get() = mock {}
        }
    }

    private suspend fun waitForState(
        uiLogic: TestErgoAuthUiLogic,
        target: ErgoAuthUiLogic.State
    ) {
        for (i in 1..250) {
            if (uiLogic.state == target) return
            if (target != ErgoAuthUiLogic.State.DONE && uiLogic.state == ErgoAuthUiLogic.State.DONE) {
                return
            }
            delay(20)
        }
    }

    private class TestErgoAuthUiLogic(
        override val coroutineScope: CoroutineScope
    ) : ErgoAuthUiLogic() {
        @Volatile
        var state: State? = null
            private set

        override fun notifyStateChanged(newState: State) {
            state = newState
        }
    }
}
