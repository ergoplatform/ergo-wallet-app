package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.*
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.LogUtils

abstract class ErgoPaySigningUiLogic : SubmitTransactionUiLogic() {
    var epsr: ErgoPaySigningRequest? = null
        private set
    var transactionInfo: TransactionInfo? = null
        private set
    var lastMessage: String? = null
        private set
    var lastMessageSeverity = MessageSeverity.NONE
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

            hasNewRequest(request, prefs)
        }
    }

    private fun hasNewRequest(request: String, prefs: PreferencesProvider) {
        // reset if we already have a request
        notifyStateChanged(State.FETCH_DATA)
        resetLastMessage()

        coroutineScope.launch(Dispatchers.IO) {
            try {
                epsr = getErgoPaySigningRequest(request).apply {
                    transactionInfo = buildTransactionInfo(ErgoApiService.getOrInit(prefs))
                }
                notifyStateChanged(State.WAIT_FOR_CONFIRMATION)
            } catch (t: Throwable) {
                LogUtils.logDebug("ErgoPay", "Error getting signing request", t)
                lastMessage = "An error occurred: ${t.message}"
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
        TODO("Not yet implemented")
    }

    override fun startColdWalletPayment(preferences: PreferencesProvider, texts: StringProvider) {
        TODO("Not yet implemented")
    }

    enum class State { WAIT_FOR_ADDRESS, FETCH_DATA, WAIT_FOR_CONFIRMATION, DONE }

    /**
     * triggers a UI refresh
     */
    abstract fun notifyStateChanged(newState: State)
}