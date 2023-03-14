package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.*
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.TransactionDbProvider
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.transactions.*
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.getMessageOrName

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
    var addressRequestCanHandleMultiple: Boolean = false
        private set
    var state = State.FETCH_DATA
        private set(value) {
            field = value
            notifyStateChanged(state)
        }

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

        state = State.FETCH_DATA

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

            fetchErgoPaySigningRequest(request, prefs, texts, database)
        }
    }

    fun canReloadFromDapp() = (state == State.WAIT_FOR_CONFIRMATION || state == State.DONE) &&
            lastRequest != null && !isErgoPayStaticRequest(lastRequest!!)

    fun reloadFromDapp(
        prefs: PreferencesProvider,
        texts: StringProvider,
        database: WalletDbProvider
    ) {
        if (!canReloadFromDapp())
            return

        fetchErgoPaySigningRequest(lastRequest!!, prefs, texts, database)
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
                changeToWaitForAddressState()
            }
        }
    }

    // keep a marker that derived address idx was set: it can be null when uninitialized or
    // when the user set it to "all addresses", so this marker can be used to distinguish these
    // states
    private var addressIdxSet = false

    fun derivedAddressIdChanged(
        prefs: PreferencesProvider,
        texts: StringProvider,
        database: WalletDbProvider
    ) {
        addressIdxSet = true
        if (lastRequest != null && epsr == null) {
            fetchErgoPaySigningRequest(
                lastRequest!!,
                prefs,
                texts,
                database,
            )
        }
    }

    private suspend fun changeToWaitForAddressState() {
        withContext(Dispatchers.IO) {// we need to fetch if multiple addresses are supported by dApp
            lastRequest?.let {
                state = State.FETCH_DATA
                addressRequestCanHandleMultiple = canErgoPayAddressRequestHandleMultiple(it)
            }
            state = State.WAIT_FOR_ADDRESS
        }
    }

    private fun fetchErgoPaySigningRequest(
        request: String,
        prefs: PreferencesProvider,
        texts: StringProvider,
        database: WalletDbProvider
    ) {
        // reset if we already have a request
        txId = null
        epsr = null
        transactionInfo = null
        resetLastMessage()
        lastRequest = request

        if (wallet == null && isErgoPayDynamicWithAddressRequest(request)) {
            state = State.WAIT_FOR_WALLET
        } else if (!addressIdxSet && derivedAddress == null
            && isErgoPayDynamicWithAddressRequest(request)
        ) {
            // if we have a address request but no address set, ask user to choose an address
            coroutineScope.launch { changeToWaitForAddressState() }
        } else {
            state = State.FETCH_DATA
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    epsr = getErgoPaySigningRequest(
                        request,
                        wallet?.let { getSigningDerivedAddresses() } ?: emptyList())
                    transactionInfo =
                        epsr?.buildTransactionInfo(getErgoApiService(prefs), prefs, texts)

                    transitionToNextStep(texts, database)
                } catch (t: Throwable) {
                    LogUtils.logDebug("ErgoPay", "Error getting signing request", t)
                    lastMessage = texts.getString(STRING_LABEL_ERROR_OCCURED, t.getMessageOrName())
                    lastMessageSeverity = MessageSeverity.ERROR
                    state = State.DONE
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

            state = State.DONE
        } else {
            val p2pkAddresses = epsr?.addressesToUse
            if (!p2pkAddresses.isNullOrEmpty()) {
                // dApp sent info regarding address to use, lets see if we have this address
                // and load it
                if (wallet == null) {
                    val walletDerivedAddress = database.loadWalletAddress(p2pkAddresses.first())
                    val walletConfig = database.loadWalletByFirstAddress(
                        walletDerivedAddress?.walletFirstAddress ?: p2pkAddresses.first()
                    )

                    walletConfig?.let {
                        initWallet(
                            database,
                            walletConfig.id,
                            walletDerivedAddress?.derivationIndex ?: 0
                        )
                    }
                }

                // check if the address(es) specified by dApp fit to our current addresses
                val walletAddresses = wallet?.let { getSigningDerivedAddresses() } ?: emptyList()
                val notFittingAddresses = p2pkAddresses.filterNot { walletAddresses.contains(it) }

                if (notFittingAddresses.isNotEmpty())
                    throw IllegalStateException(
                        texts.getString(
                            STRING_LABEL_ERGO_PAY_WRONG_ADDRESS,
                            notFittingAddresses.first()
                        )
                    )
            }

            if (wallet != null)
                state = State.WAIT_FOR_CONFIRMATION
            else
                state = State.WAIT_FOR_WALLET
        }
    }

    // override in unit tests
    protected open fun getErgoApiService(prefs: PreferencesProvider) =
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
                    val signingResult = ErgoFacade.signSerializedErgoTx(
                        signingRequest, signingSecrets,
                        derivedAddresses, texts
                    )
                    signingSecrets.clearMemory()
                    if (signingResult.success) {
                        ergoTxResult = ErgoFacade.sendSignedErgoTx(
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

    override fun startColdWalletOrMultisigPayment(
        preferences: PreferencesProvider,
        texts: StringProvider,
        transactionDbProvider: TransactionDbProvider,
    ) {
        resetLastMessage()
        epsr?.reducedTx?.let {
            notifyUiLocked(true)
            coroutineScope.launch(Dispatchers.IO) {
                val serializedTx = ErgoFacade.buildPromptSigningResultFromErgoPayRequest(
                    it,
                    getSigningDerivedAddresses().first(),
                    preferences,
                    texts
                )
                notifyUiLocked(false)
                startMultisigOrColdWalletPaymentPrompt(serializedTx, transactionDbProvider)
            }
        }
    }

    override fun notifyHasSubmittedTxId(txId: String) {
        this.txId = txId
        state = State.DONE
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

    fun showRatingPrompt(): Boolean =
        txId != null && getDoneSeverity() != MessageSeverity.ERROR


    enum class State { WAIT_FOR_WALLET, WAIT_FOR_ADDRESS, FETCH_DATA, WAIT_FOR_CONFIRMATION, DONE }

    /**
     * triggers a UI refresh
     */
    abstract fun notifyStateChanged(newState: State)
}