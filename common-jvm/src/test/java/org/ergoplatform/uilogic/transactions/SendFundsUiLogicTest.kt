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
            val uiLogic = TestSendFundsUiLogic()

            val walletId = 1
            val walletDbProvider = mock<WalletDbProvider> {
            }
            whenever(walletDbProvider.loadWalletWithStateById(0)).thenReturn(wallet)

            uiLogic.initWallet(walletDbProvider, walletId, 0, null)

            delay(100)

            var checkResponse = uiLogic.checkCanMakePayment()
            assertFalse(checkResponse.canPay)
            assertTrue(checkResponse.receiverError)
            assertTrue(checkResponse.amountError)
            assertFalse(checkResponse.tokenError)

            uiLogic.newTokenChoosen(token.tokenId!!)

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

    private val firstAddress = "address"
    private val token = WalletToken(
        1,
        firstAddress,
        firstAddress,
        "74251ce2cb4eb2024a1a155e19ad1d1f58ff8b9e6eb034a3bb1fd58802757d23",
        1,
        0,
        "testtoken"
    )
    private val wallet = Wallet(
        WalletConfig(1, "test", firstAddress, 0, null, false),
        listOf(WalletState(firstAddress, firstAddress, 1000L * 1000 * 1000, 0)),
        listOf(token),
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

        override fun notifyHasSigningPromptData(signingPrompt: String?) {

        }

    }
}