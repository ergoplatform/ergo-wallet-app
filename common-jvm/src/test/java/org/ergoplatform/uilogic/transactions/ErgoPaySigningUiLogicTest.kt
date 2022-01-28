package org.ergoplatform.uilogic.transactions

import junit.framework.TestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.ergoplatform.ErgoApi
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.TestPreferencesProvider
import org.ergoplatform.TestStringProvider
import org.ergoplatform.explorer.client.model.OutputInfo
import org.ergoplatform.explorer.client.model.TotalBalance
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.transactions.STATIC_ERGO_PAY_URI
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.STRING_LABEL_ERROR_OCCURED
import org.ergoplatform.uilogic.STRING_LABEL_MESSAGE_FROM_DAPP
import org.ergoplatform.uilogic.TestUiWallet
import org.ergoplatform.utils.LogUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.EmptyCoroutineContext


class ErgoPaySigningUiLogicTest : TestCase() {

    override fun setUp() {
        LogUtils.logDebug = true
    }

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
            buildUiLogicWithWallet(STATIC_ERGO_PAY_URI, apiFailure = true).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))
            }
        }
    }

    fun testDynamic() {
        val server = MockWebServer()
        val RESPONSE_SUCCESS =
            "{\"message\":\"Here is your 1 ERG round trip.\",\"messageSeverity\":\"INFORMATION\",\"address\":\"${TestUiWallet.firstAddress}\",\"reducedTx\":\"vAIBsY8KufROXCUQyk05e7UCm4hdh-afd4BXlhyy-i7xo8YAAAACUUCDoXD8c0BxwHdI_0RJQGBmVDF71RaGUSDtcClSqxuW1lsZOJnBVNdbw6VbLTwjEfCfArnW7S2skTyxDDgwgAOAlOvcAwAIzQKya10Oh0WJx8V1Smpq9AgIH___4xEYT0SiW4DH8rxitYSUCQAAwIQ9EAUEAAQADjYQAgSQAQjNAnm-Zn753LusVaBilc6HCwcCm_zbLc4o2VnygVsW-BeY6gLRkqOajMenAXMAcwEQAQIEAtGWgwMBk6OMx7KlcwAAAZPCsqVzAQB0cwJzA4MBCM3urJOxpXMEhJQJAADZptHA6wkACM0CsmtdDodFicfFdUpqavQICB___-MRGE9EoluAx_K8YrWElAkCAPgKAeC00S8AzQKya10Oh0WJx8V1Smpq9AgIH___4xEYT0SiW4DH8rxitZ1PrGY=\"}"
        server.enqueue(MockResponse().setBody(RESPONSE_SUCCESS))
        server.enqueue(MockResponse().setBody("hello, world!"))
        server.enqueue(MockResponse().setBody("{\"wrongmessage\":\"Out of order. Please try again later.\",\"messageSeverity\":\"WARNING\"}"))
        server.enqueue(MockResponse().setBody("{\"message\":\"Here is your 1 ERG round trip.\",\"messageSeverity\":\"INFORMATION\",\"address\":\"someAddressNotHere\",\"reducedTx\":\"vAIBsY8KufROXCUQyk05e7UCm4hdh-afd4BXlhyy-i7xo8YAAAACUUCDoXD8c0BxwHdI_0RJQGBmVDF71RaGUSDtcClSqxuW1lsZOJnBVNdbw6VbLTwjEfCfArnW7S2skTyxDDgwgAOAlOvcAwAIzQKya10Oh0WJx8V1Smpq9AgIH___4xEYT0SiW4DH8rxitYSUCQAAwIQ9EAUEAAQADjYQAgSQAQjNAnm-Zn753LusVaBilc6HCwcCm_zbLc4o2VnygVsW-BeY6gLRkqOajMenAXMAcwEQAQIEAtGWgwMBk6OMx7KlcwAAAZPCsqVzAQB0cwJzA4MBCM3urJOxpXMEhJQJAADZptHA6wkACM0CsmtdDodFicfFdUpqavQICB___-MRGE9EoluAx_K8YrWElAkCAPgKAeC00S8AzQKya10Oh0WJx8V1Smpq9AgIH___4xEYT0SiW4DH8rxitZ1PrGY=\"}"))
        server.enqueue(MockResponse().setBody("{\"message\":\"Out of order. Please try again later.\",\"messageSeverity\":\"WARNING\"}"))
        server.enqueue(MockResponse().setBody(RESPONSE_SUCCESS))
        server.enqueue(MockResponse().setBody(RESPONSE_SUCCESS))

        runBlocking {
            server.start()
            val url = "ergopay://" + server.url("/").toString().substringAfter("http://")

            // acutal thing going through
            buildUiLogicWithWallet(url).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNotNull(epsr?.message)
                assertEquals(MessageSeverity.INFORMATION, epsr?.messageSeverity)
                assertEquals(TestUiWallet.firstAddress, epsr?.p2pkAddress)
                assertNull(epsr?.replyToUrl)

            }

            // hello, world will fail
            buildUiLogicWithWallet(url).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))

            }

            // response without message and without reducedTx => fail
            buildUiLogicWithWallet(url).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))

            }

            // wrong address - fail
            buildUiLogicWithWallet(url).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNotNull(epsr)
                assertNotNull(transactionInfo)
                assertEquals(MessageSeverity.ERROR, getDoneSeverity())
                assertEquals(STRING_LABEL_ERROR_OCCURED, getDoneMessage(TestStringProvider()))
            }

            // no reduced tx, but information message
            buildUiLogicWithWallet(url).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.DONE, state)
                assertNotNull(epsr)
                assertNull(transactionInfo)
                assertEquals(MessageSeverity.WARNING, getDoneSeverity())
                assertEquals(STRING_LABEL_MESSAGE_FROM_DAPP, getDoneMessage(TestStringProvider()))
            }

            // request with address - goes through because only one address on wallet
            buildUiLogicWithWallet("$url?address=#P2PK_ADDRESS#").apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNotNull(epsr?.message)
            }

            // request with address, but two derived addresses configured: ask user
            buildUiLogicWithWallet("$url?address=#P2PK_ADDRESS#", twoAddresses = true).apply {
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_ADDRESS, state)
                assertNull(epsr)
                assertNull(transactionInfo)
                derivedAddressIdx = 0
                hasNewRequest(lastRequest!!, TestPreferencesProvider(), TestStringProvider())
                assertEquals(ErgoPaySigningUiLogic.State.FETCH_DATA, state)
                waitWhileFetching(this)
                assertEquals(ErgoPaySigningUiLogic.State.WAIT_FOR_CONFIRMATION, state)
                assertNotNull(epsr)
                assertNotNull(epsr?.reducedTx)
                assertNotNull(epsr?.message)
            }

            server.shutdown()
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
        apiFailure: Boolean = false,
        twoAddresses: Boolean = false
    ): TestErgoPaySigningUiLogic {
        val uiLogic = TestErgoPaySigningUiLogic()
        uiLogic.ergoApiFailure = apiFailure

        val walletId = 1

        val prefs = TestPreferencesProvider()
        uiLogic.init(
            request, walletId, -1,
            if (twoAddresses) TestUiWallet.getSingleWalletTwoAddressesDbProvider(walletId)
            else TestUiWallet.getSingleWalletSingleAddressDbProvider(walletId),
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