package org.ergoplatform.uilogic.transactions

import junit.framework.TestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.ResponseBody
import org.ergoplatform.ErgoApi
import org.ergoplatform.MessageSeverity
import org.ergoplatform.TestPreferencesProvider
import org.ergoplatform.TestStringProvider
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.explorer.client.model.TotalBalance
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.transactions.STATIC_ERGO_PAY_URI
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.STRING_LABEL_ERROR_OCCURED
import org.ergoplatform.uilogic.TestUiWallet
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.EmptyCoroutineContext

class ErgoPaySigningUiLogicTest : TestCase() {

    fun testStatic() {
        runBlocking {
            // test everything is fine
            buildUiLogicWithWallet(STATIC_ERGO_PAY_URI).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)

                waitWhileFetching(this)

                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNull(epsr?.message)
                assertEquals(MessageSeverity.NONE, epsr?.messageSeverity)
                assertNull(epsr?.p2pkAddress)
                assertNull(epsr?.replyToUrl)
            }

            // test invalid static reduced tx given
            buildUiLogicWithWallet("ergopay:thisisinvalid").apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))
            }

            // test with invalid boxes
            buildUiLogicWithWallet(STATIC_ERGO_PAY_URI, true).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNull(epsr)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))
            }
        }
    }

    private suspend fun waitWhileFetching(uiLogic: TestErgoPaySigningUiLogic) {
        for (i in 1..50) {
            delay(100)
            if (uiLogic.state != ErgoPaySigningUiLogic.State.FETCH_DATA) break
        }
    }

    private suspend fun buildUiLogicWithWallet(
        request: String,
        apiFailure: Boolean = false
    ): TestErgoPaySigningUiLogic {
        val uiLogic = TestErgoPaySigningUiLogic()
        uiLogic.ergoApiFailure = apiFailure

        val walletId = 1

        val prefs = TestPreferencesProvider()
        uiLogic.init(
            request, walletId, -1, TestUiWallet.getWalletDbProvider(walletId),
            prefs, TestStringProvider()
        )

        return uiLogic
    }

    class TestErgoPaySigningUiLogic : ErgoPaySigningUiLogic() {
        var state: State? = null
            private set

        var ergoApiFailure = false

        override val coroutineScope: CoroutineScope
            get() = CoroutineScope(EmptyCoroutineContext)

        override fun notifyStateChanged(newState: State) {
            state = newState
        }

        override fun notifyWalletStateLoaded() {

        }

        override fun notifyDerivedAddressChanged() {

        }

        override fun notifyUiLocked(locked: Boolean) {

        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {

        }

        override fun notifyHasSigningPromptData(signingPrompt: String) {

        }

        override fun getErgoApiService(prefs: PreferencesProvider): ErgoApi {
            return object : ErgoApi {
                override fun getTotalBalanceForAddress(publicAddress: String): Call<TotalBalance> {
                    error("Not implemented")
                }

                override fun getBoxInformation(boxId: String): Call<OutputInfo> {
                    return object : Call<OutputInfo> {
                        override fun clone(): Call<OutputInfo> {
                            error("Not implemented")
                        }

                        override fun execute(): Response<OutputInfo> {
                            return if (ergoApiFailure)
                                Response.error(404, ResponseBody.create(null, "Moved"))
                            else
                                Response.success(OutputInfo().apply {
                                    setBoxId(boxId)
                                    address = wallet?.walletConfig?.firstAddress
                                    value = 1000L * 1000L * 1000L
                                    assets = emptyList()
                                })
                        }

                        override fun enqueue(callback: Callback<OutputInfo>) {
                            error("Not implemented")
                        }

                        override fun isExecuted(): Boolean {
                            error("Not implemented")
                        }

                        override fun cancel() {
                            error("Not implemented")
                        }

                        override fun isCanceled(): Boolean {
                            error("Not implemented")
                        }

                        override fun request(): Request {
                            error("Not implemented")
                        }

                    }
                }

            }
        }
    }
}