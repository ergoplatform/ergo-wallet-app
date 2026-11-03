package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.ErgoAmount
import org.ergoplatform.TestPreferencesProvider
import org.ergoplatform.appkit.Parameters
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.TestUiWallet
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Call
import retrofit2.Response
import kotlin.coroutines.EmptyCoroutineContext

class SendFundsUiLogicTest {

    @Test
    fun convertsWaitMillisecondsBeforeNarrowingToMinutes() {
        assertEquals(0, waitTimeMillisToMinutes(0L))
        assertEquals(1, waitTimeMillisToMinutes(60_000L))
        assertEquals(3, waitTimeMillisToMinutes(120_001L))
        assertEquals(35_792, waitTimeMillisToMinutes(Int.MAX_VALUE.toLong() + 1L))
        assertEquals(Int.MAX_VALUE, waitTimeMillisToMinutes(Long.MAX_VALUE))
    }

    @Test
    fun appliesNodeWaitMillisecondsToSelectedFee() {
        val waitTimeCall = mock<Call<Long>>()
        whenever(waitTimeCall.execute()).thenReturn(Response.success(Int.MAX_VALUE.toLong() + 1L))
        val apiService = mock<ApiServiceManager>()
        val selectedFee = Parameters.MinFee + 1L
        whenever(apiService.getExpectedWaitTime(selectedFee, 1000)).thenReturn(waitTimeCall)

        val uiLogic = TestSendFundsUiLogic()
        uiLogic.setNewFeeAmount(ErgoAmount(selectedFee), apiService)
        val observedMinutes = runBlocking {
            withTimeout(5_000L) { uiLogic.feeWaitTimeReady.await() }
        }

        assertEquals(35_792, observedMinutes)
        assertEquals(35_792, uiLogic.feeMinutesToWait)
        assertEquals(selectedFee, uiLogic.feeAmount.nanoErgs)
    }

    @Test
    fun checkCanMakePayment() {
        runBlocking {
            val uiLogic = buildUiLogicWithWallet()
            val preferences = TestPreferencesProvider()

            var checkResponse = uiLogic.checkCanMakePayment(preferences)
            assertFalse(checkResponse.canPay)
            assertTrue(checkResponse.receiverError)
            assertTrue(checkResponse.amountError)
            assertFalse(checkResponse.tokenError)

            uiLogic.newTokenChosen(TestUiWallet.token.tokenId!!)

            // when a token is chosen there's no amount error any more
            checkResponse = uiLogic.checkCanMakePayment(preferences)
            assertFalse(checkResponse.amountError)
            assertTrue(checkResponse.tokenError)

            // only if we've set an amount that is not possible
            uiLogic.setAmountToSendErg(ErgoAmount(Parameters.MinChangeValue - 1))
            checkResponse = uiLogic.checkCanMakePayment(preferences)
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
        uiLogic.initWallet(TestUiWallet.getSingleWalletSingleAddressDbProvider(walletId), mock<ApiServiceManager> {}, walletId, 0, null)

        delay(100)
        return uiLogic
    }

    class TestSendFundsUiLogic : SendFundsUiLogic() {

        val feeWaitTimeReady = CompletableDeferred<Int>()

        override val coroutineScope: CoroutineScope
            get() = CoroutineScope(EmptyCoroutineContext)

        override fun notifyWalletStateLoaded() {

        }

        override fun notifyDerivedAddressChanged() {

        }

        override fun notifyTokensChosenChanged() {

        }

        override fun onNotifySuggestedFees() {

        }

        override fun notifyAmountsChanged() {
            feeMinutesToWait?.let { feeWaitTimeReady.complete(it) }
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

        override fun notifyHasPreparedTx(preparedTx: TransactionInfo) {

        }

        override fun showErrorMessage(message: String) {

        }

    }
}