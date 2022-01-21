package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.ergoplatform.ErgoAmount
import org.ergoplatform.appkit.Parameters
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.TestUiWallet
import org.junit.Assert.*

import org.junit.Test
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

            uiLogic.newTokenChosen(TestUiWallet.token.tokenId!!)

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
            val token = TestUiWallet.token
            val singularToken = TestUiWallet.singularToken

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
        uiLogic.initWallet(TestUiWallet.getSingleWalletSingleAddressDbProvider(walletId), walletId, 0, null)

        delay(100)
        return uiLogic
    }

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

        override fun showErrorMessage(message: String) {

        }

    }
}