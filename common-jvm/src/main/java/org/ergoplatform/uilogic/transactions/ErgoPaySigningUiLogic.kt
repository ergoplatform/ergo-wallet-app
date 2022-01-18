package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.*
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.transactions.SendTransactionResult
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.STRING_DESC_TRANSACTION_SEND
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.getMessageOrName

abstract class ErgoPaySigningUiLogic : SubmitTransactionUiLogic() {
    var epsr: ErgoPaySigningRequest? = null
        private set
    var transactionInfo: TransactionInfo? = null
        private set
    var lastMessage: String? = null
        private set
    var lastMessageSeverity = MessageSeverity.NONE
        private set
    var txId: String? = null
        private set

    fun init(
        request: String,
        walletId: Int,
        derivationIndex: Int,
        database: WalletDbProvider,
        prefs: PreferencesProvider
    ) {
        // prevent reinitialization on device rotation
        if (wallet != null)
            return

        notifyStateChanged(State.FETCH_DATA)

        coroutineScope.launch {
            initWallet(database, walletId, derivationIndex)

            // TODO Ergo Pay in case of need for address
            // notifyStateChanged(State.WAIT_FOR_ADDRESS)

            // fake a request
//            withContext(Dispatchers.IO) {
//                val serializedTx = prepareSerializedErgoTx(
//                    Address.create(wallet!!.walletConfig.firstAddress), 1000L * 1000L * 500,
//                    emptyList(),
//                    listOf(Address.create(wallet!!.walletConfig.firstAddress)),
//                    prefs, object : StringProvider {
//                        override fun getString(stringId: String): String {
//                            return stringId
//                        }
//
//                        override fun getString(stringId: String, vararg formatArgs: Any): String {
//                            return stringId
//                        }
//
//                    }
//                )
//                hasNewRequest(
//                    "ergopay:" + Base64.getUrlEncoder().encodeToString(serializedTx.serializedTx!!),
//                    prefs
//                )
//            } ?:

            hasNewRequest(request, prefs)
        }
    }

    private fun hasNewRequest(request: String, prefs: PreferencesProvider) {
        // reset if we already have a request
        notifyStateChanged(State.FETCH_DATA)
        txId = null
        resetLastMessage()

        coroutineScope.launch(Dispatchers.IO) {
            try {
                epsr = getErgoPaySigningRequest(request).apply {
                    transactionInfo = buildTransactionInfo(ErgoApiService.getOrInit(prefs))
                }
                notifyStateChanged(State.WAIT_FOR_CONFIRMATION)
            } catch (t: Throwable) {
                // TODO Ergo Pay show a Repeat button for user convenience when it is an IOException
                LogUtils.logDebug("ErgoPay", "Error getting signing request", t)
                lastMessage = "An error occurred:\n${t.getMessageOrName()}"
                lastMessageSeverity = MessageSeverity.ERROR
                notifyStateChanged(State.DONE)
            }
        }
    }

    private fun resetLastMessage() {
        lastMessage = null
        lastMessageSeverity = MessageSeverity.NONE
    }

    override fun startPaymentWithMnemonicAsync(
        mnemonic: String,
        preferences: PreferencesProvider,
        texts: StringProvider
    ) {
        resetLastMessage()
        notifyUiLocked(true)
        epsr?.reducedTx?.let { signingRequest ->
            val derivedAddresses = getSigningDerivedAddressesIndices()

            coroutineScope.launch {
                val ergoTxResult: SendTransactionResult
                withContext(Dispatchers.IO) {
                    val signingResult = signSerializedErgoTx(
                        signingRequest, mnemonic, "",
                        derivedAddresses, texts
                    )
                    if (signingResult.success) {
                        ergoTxResult = sendSignedErgoTx(
                            signingResult.serializedTx!!,
                            preferences, texts
                        )
                    } else {
                        ergoTxResult =
                            SendTransactionResult(false, errorMsg = signingResult.errorMsg)
                    }

                }
                notifyUiLocked(false)

                if (ergoTxResult.success) {
                    NodeConnector.getInstance().invalidateCache()
                    notifyHasTxId(ergoTxResult.txId!!)
                }
                notifyHasErgoTxResult(ergoTxResult)
            }

            notifyUiLocked(true)
        }
    }

    override fun startColdWalletPayment(preferences: PreferencesProvider, texts: StringProvider) {
        resetLastMessage()
        epsr?.reducedTx?.let {
            notifyUiLocked(true)
            coroutineScope.launch(Dispatchers.IO) {
                val serializedTx = buildPromptSigningResultFromErgoPayRequest(
                    it,
                    getSigningDerivedAddresses().first(),
                    preferences,
                    texts
                )
                notifyUiLocked(false)
                startColdWalletPaymentPrompt(serializedTx)
            }
        }
    }

    override fun notifyHasTxId(txId: String) {
        this.txId = txId
        notifyStateChanged(State.DONE)
    }

    fun getDoneMessage(texts: StringProvider): String =
        lastMessage ?: (texts.getString(STRING_DESC_TRANSACTION_SEND) + txId?.let { "\n\n$it" })

    fun getDoneSeverity(): MessageSeverity =
        if (lastMessage == null && txId != null) MessageSeverity.INFORMATION else lastMessageSeverity

    enum class State { WAIT_FOR_ADDRESS, FETCH_DATA, WAIT_FOR_CONFIRMATION, DONE }

    /**
     * triggers a UI refresh
     */
    abstract fun notifyStateChanged(newState: State)
}