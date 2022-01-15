package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.ergoplatform.ErgoAmount
import org.ergoplatform.appkit.Parameters
import org.ergoplatform.persistance.*
import org.ergoplatform.transactions.TransactionResult
import org.junit.Assert.*

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.coroutines.EmptyCoroutineContext

class SendFundsUiLogicTest {

    @Test
    fun checkCanMakePayment() {
        runBlocking {
            val uiLogic = buildUiLogicWithWallet()

            var checkResponse = uiLogic.checkCanMakePayment()
            assertFalse(checkResponse.canPay)
            assertTrue(checkResponse.receiverError)
            assertTrue(checkResponse.amountError)
            assertFalse(checkResponse.tokenError)

            uiLogic.newTokenChosen(token.tokenId!!)

            // when a token is chosen there's no amount error any more
            checkResponse = uiLogic.checkCanMakePayment()
            assertFalse(checkResponse.amountError)
            assertTrue(checkResponse.tokenError)

            // only if we've set an amount that is not possible
            uiLogic.amountToSend = ErgoAmount(Parameters.MinChangeValue - 1)
            checkResponse = uiLogic.checkCanMakePayment()
            assertTrue(checkResponse.amountError)
        }
    }

    @Test
    fun testTokenAmounts() {
        runBlocking {
            val uiLogic = buildUiLogicWithWallet()
            uiLogic.newTokenChosen(token.tokenId!!)
            uiLogic.newTokenChosen(singularToken.tokenId!!)

            assertEquals(2, uiLogic.tokensChosen.size)
            assertEquals(1L, uiLogic.tokensChosen.get(singularToken.tokenId)?.value)
            assertEquals(0L, uiLogic.tokensChosen.get(token.tokenId)?.value)

            val uiLogic2 = buildUiLogicWithWallet()
            val qrHash = HashMap<String, String>()
            qrHash.put(singularToken.tokenId!!, "")
            qrHash.put(token.tokenId!!, "")
            uiLogic2.addTokensFromPaymentRequest(qrHash)
            assertEquals(1L, uiLogic2.tokensChosen.get(singularToken.tokenId)?.value)
            assertEquals(0L, uiLogic2.tokensChosen.get(token.tokenId)?.value)
            val uiLogic3 = buildUiLogicWithWallet()
            qrHash.clear()
            qrHash.put(singularToken.tokenId!!, "2")
            qrHash.put(token.tokenId!!, "2")
            uiLogic3.addTokensFromPaymentRequest(qrHash)
            assertEquals(2L, uiLogic3.tokensChosen.get(singularToken.tokenId)?.value)
            assertEquals(2L, uiLogic3.tokensChosen.get(token.tokenId)?.value)
        }
    }

    private suspend fun buildUiLogicWithWallet(): TestSendFundsUiLogic {
        val uiLogic = TestSendFundsUiLogic()

        val walletId = 1
        val walletDbProvider = mock<WalletDbProvider> {
        }
        whenever(walletDbProvider.loadWalletWithStateById(walletId)).thenReturn(wallet)

        uiLogic.initWallet(walletDbProvider, walletId, 0, null)

        delay(100)
        return uiLogic
    }

    private val firstAddress = "address"
    private val token = WalletToken(
        1,
        firstAddress,
        firstAddress,
        "74251ce2cb4eb2024a1a155e19ad1d1f58ff8b9e6eb034a3bb1fd58802757d23",
        10,
        0,
        "testtoken"
    )
    private val singularToken = WalletToken(
        2,
        firstAddress,
        firstAddress,
        "ba5856162d6342d2a0072f464a5a8b62b4ac4dd77195bec18c6bf268c2def831",
        1,
        0,
        "nft"
    )
    private val wallet = Wallet(
        WalletConfig(1, "test", firstAddress, 0, null, false),
        listOf(WalletState(firstAddress, firstAddress, 1000L * 1000 * 1000, 0)),
        listOf(token, singularToken),
        emptyList()
    )

    class TestSendFundsUiLogic : SendFundsUiLogic() {

        override val coroutineScope: CoroutineScope
            get() = CoroutineScope(EmptyCoroutineContext)

        override fun notifyWalletStateLoaded() {

        }

        override fun notifyDerivedAddressChanged() {

        }

        override fun notifyTokensChosenChanged() {

        }

        override fun notifyAmountsChanged() {

        }

        override fun notifyBalanceChanged() {

        }

        override fun notifyUiLocked(locked: Boolean) {

        }

        override fun notifyHasTxId(txId: String) {

        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {

        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {

        }

    }
}