package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.*
import org.ergoplatform.api.ErgoExplorerApi
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.transactions.*
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.getMessageOrName
import java.lang.IllegalArgumentException

abstract class ErgoPaySigningUiLogic : SubmitTransactionUiLogic() {
    private var isInitialized = false
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
    var lastRequest: String? = null
        private set

    fun init(
        request: String,
        walletId: Int,
        derivationIndex: Int,
        database: WalletDbProvider,
        prefs: PreferencesProvider,
        texts: StringProvider
    ) {
        // prevent reinitialization on device rotation
        if (isInitialized)
            return
        isInitialized = true

        notifyStateChanged(State.FETCH_DATA)

        coroutineScope.launch {
            if (walletId >= 0) {
                initWallet(database, walletId, derivationIndex)
            } else {
                // if there is only a single wallet configured, we can auto-choose
                withContext(Dispatchers.IO) {
                    val allConfigs = database.getAllWalletConfigsSynchronous()
                    if (allConfigs.size == 1) initWallet(database, allConfigs.first().id, -1)
                }
            }

            // uncomment this code to fake a request to a server URL. This will build an
            // ergo pay signing request that sends 0.5 ERG from and to the first address of the
            // current wallet
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

            fetchErgoPaySigningRequest(request, prefs, texts, database)
        }
    }

    /**
     * called when wallet id is set in state WAIT_FOR_ADDRESS
     * @return true if a derived address is needed
     */
    fun setWalletId(
        walletId: Int,
        prefs: PreferencesProvider,
        texts: StringProvider,
        database: WalletDbProvider
    ) {
        if (wallet != null && wallet?.walletConfig?.id != walletId)
            throw IllegalArgumentException("Cannot change wallet id when ui logic already initialized")

        coroutineScope.launch {
            initWallet(database, walletId, -1)
            if (epsr != null)
                transitionToNextStep(texts, database)
            else if (derivedAddress != null) {
                derivedAddressIdChanged(prefs, texts, database)
            } else {
                notifyStateChanged(State.WAIT_FOR_ADDRESS)
            }
        }
    }

    fun derivedAddressIdChanged(
        prefs: PreferencesProvider,
        texts: StringProvider,
        database: WalletDbProvider
    ) {
        if (lastRequest != null && epsr == null) {
            fetchErgoPaySigningRequest(
                lastRequest!!,
                prefs,
                texts,
                database,
            )
        }
    }

    fun fetchErgoPaySigningRequest(
        request: String,
        prefs: PreferencesProvider,
        texts: StringProvider,
        database: WalletDbProvider
    ) {
        // reset if we already have a request
        txId = null
        resetLastMessage()
        lastRequest = request

        if (wallet == null && isErgoPayDynamicWithAddressRequest(request)) {
            notifyStateChanged(State.WAIT_FOR_WALLET)
        } else if (derivedAddress == null && isErgoPayDynamicWithAddressRequest(request)) {
            // if we have a address request but no address set, ask user to choose an address
            notifyStateChanged(State.WAIT_FOR_ADDRESS)
        } else {
            notifyStateChanged(State.FETCH_DATA)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    epsr = getErgoPaySigningRequest(request, derivedAddress?.publicAddress)
                    transactionInfo = epsr?.buildTransactionInfo(getErgoApiService(prefs))

                    transitionToNextStep(texts, database)
                } catch (t: Throwable) {
                    // TODO Ergo Pay show a Repeat button for user convenience when it is an IOException
                    LogUtils.logDebug("ErgoPay", "Error getting signing request", t)
                    lastMessage = texts.getString(STRING_LABEL_ERROR_OCCURED, t.getMessageOrName())
                    lastMessageSeverity = MessageSeverity.ERROR
                    notifyStateChanged(State.DONE)
                }
            }
        }
    }

    private suspend fun transitionToNextStep(
        texts: StringProvider,
        database: WalletDbProvider
    ) {
        if (transactionInfo == null) {
            epsr?.message?.let {
                lastMessage = texts.getString(STRING_LABEL_MESSAGE_FROM_DAPP, it)
                lastMessageSeverity = epsr?.messageSeverity ?: MessageSeverity.NONE
            }

            notifyStateChanged(State.DONE)
        } else {
            epsr?.p2pkAddress?.let { p2pkAddress ->
                // dApp sent info regarding address to use, lets see if we have this address
                // and load it
                if (wallet == null) {
                    val walletDerivedAddress = database.loadWalletAddress(p2pkAddress)
                    val walletConfig = database.loadWalletByFirstAddress(
                        walletDerivedAddress?.walletFirstAddress ?: p2pkAddress
                    )

                    walletConfig?.let {
                        initWallet(
                            database,
                            walletConfig.id,
                            walletDerivedAddress?.derivationIndex ?: 0
                        )
                    }
                }

                // check if the address specified by dApp was chosen
                val hasAddress = wallet?.let {
                    getSigningDerivedAddresses().any {
                        p2pkAddress.equals(it, true)
                    }
                } ?: false

                if (!hasAddress)
                    throw IllegalStateException(
                        texts.getString(
                            STRING_LABEL_ERGO_PAY_WRONG_ADDRESS,
                            p2pkAddress
                        )
                    )
            }

            if (wallet != null)
                notifyStateChanged(State.WAIT_FOR_CONFIRMATION)
            else
                notifyStateChanged(State.WAIT_FOR_WALLET)
        }
    }

    // override in unit tests
    protected open fun getErgoApiService(prefs: PreferencesProvider): ErgoExplorerApi =
        ApiServiceManager.getOrInit(prefs)

    private fun resetLastMessage() {
        lastMessage = null
        lastMessageSeverity = MessageSeverity.NONE
    }

    override fun startPaymentWithMnemonicAsync(
        signingSecrets: SigningSecrets,
        preferences: PreferencesProvider,
        texts: StringProvider,
        db: IAppDatabase,
    ) {
        resetLastMessage()
        notifyUiLocked(true)
        epsr?.reducedTx?.let { signingRequest ->
            val derivedAddresses = getSigningDerivedAddressesIndices()

            coroutineScope.launch {
                val ergoTxResult: SendTransactionResult
                withContext(Dispatchers.IO) {
                    val signingResult = signSerializedErgoTx(
                        signingRequest, signingSecrets,
                        derivedAddresses, texts
                    )
                    signingSecrets.clearMemory()
                    if (signingResult.success) {
                        ergoTxResult = sendSignedErgoTx(
                            signingResult.serializedTx!!,
                            preferences, texts
                        )
                    } else {
                        ergoTxResult =
                            SendTransactionResult(false, errorMsg = signingResult.errorMsg)
                    }

                    notifyUiLocked(false)
                    transactionSubmitted(
                        ergoTxResult,
                        db.transactionDbProvider,
                        preferences,
                        transactionInfo
                    )
                }
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
        sendReplyToDapp(txId)
    }

    private fun sendReplyToDapp(txId: String) {
        epsr?.replyToUrl?.let {
            // fire & forget type of reply
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    epsr?.sendReplyToDApp(txId)
                } catch (t: Throwable) {
                    // ignore
                }
            }
        }
    }

    fun getDoneMessage(texts: StringProvider): String =
        lastMessage ?: txId?.let {
            texts.getString(STRING_DESC_TRANSACTION_SEND) + "\n\n$it"
        } ?: "Unknown error occurred."

    fun getDoneSeverity(): MessageSeverity =
        if (lastMessage == null && txId != null) MessageSeverity.INFORMATION else
            lastMessage?.let { lastMessageSeverity } ?: MessageSeverity.ERROR

    enum class State { WAIT_FOR_WALLET, WAIT_FOR_ADDRESS, FETCH_DATA, WAIT_FOR_CONFIRMATION, DONE }

    /**
     * triggers a UI refresh
     */
    abstract fun notifyStateChanged(newState: State)
}