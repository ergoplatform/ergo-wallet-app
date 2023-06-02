package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.*
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.ErgoToken
import org.ergoplatform.appkit.Parameters
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.tokens.TokenInfoManager
import org.ergoplatform.tokens.isSingularToken
import org.ergoplatform.transactions.*
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.formatFiatToString
import org.ergoplatform.wallet.*
import kotlin.math.max

abstract class SendFundsUiLogic : SubmitTransactionUiLogic() {

    var receiverAddress: String = ""
        set(value) {
            field = value
            calcGrossAmount()
        }
    var message: String = ""
    val maxMessageLength = 1000 // we allow only 1k characters to be sent - no pollution wanted

    /**
     * amount to send, entered by user
     */
    val amountToSend: ErgoAmount get() = _amountToSend.ergAmount
    private val _amountToSend = ErgoOrFiatAmount()
    val inputIsFiat: Boolean get() = _amountToSend.inputIsFiat
    val inputAmountString get() = _amountToSend.getInputAmountString()

    private val feeTxSize =
        1000 // we use constant size of 1000 here, our user-made transactions are small
    var feeAmount: ErgoAmount = ErgoAmount(Parameters.MinFee)
        private set
    var feeMinutesToWait: Int? = null
        private set
    private var minutesToWaitFetchJob: Job? = null

    var suggestedFeeItems: List<SuggestedFee> = emptyList()
        private set
    private var suggestedFeeJob: Job? = null

    var grossAmount: ErgoAmount = ErgoAmount.ZERO
        private set
    var balance: ErgoAmount = ErgoAmount.ZERO
        private set

    val tokensAvail: HashMap<String, WalletToken> = HashMap()
    val tokensChosen: HashMap<String, ErgoToken> = HashMap()
    val tokensInfo: HashMap<String, TokenInformation> = HashMap()

    private val paymentRequestWarnings = ArrayList<PaymentRequestWarning>()

    fun initWallet(
        database: IAppDatabase,
        ergoApiService: ApiServiceManager,
        walletId: Int,
        derivationIdx: Int,
        paymentRequest: String?
    ) {
        val firstInit = wallet == null

        // on first init, we read an send payment request. Don't do it again on device rotation
        // to not mess with user inputs
        val content: PaymentRequest?
        if (firstInit) {
            content = paymentRequest?.let { parsePaymentRequest(paymentRequest) }
            content?.let {
                receiverAddress = content.address
                setAmountToSendErg(content.amount)
                message = content.description
            }
        } else content = null

        coroutineScope.launch {
            initWallet(database.walletDbProvider, walletId, derivationIdx)

            content?.let {
                addTokensFromPaymentRequest(content.tokens)
                notifyTokensChosenChanged()
            }

            if (firstInit) {
                wallet?.getTokensForAllAddresses()?.forEach {
                    it.tokenId?.let {
                        TokenInfoManager.getInstance()
                            .getTokenInformation(it, database.tokenDbProvider, ergoApiService)
                            ?.let {
                                synchronized(tokensInfo) {
                                    tokensInfo.put(it.tokenId, it)
                                }
                            }
                    }
                }
                fetchFeeWaitTime(ergoApiService)
            }
        }
        calcGrossAmount()
    }

    private fun fetchFeeWaitTime(ergoApiService: ApiServiceManager) {
        minutesToWaitFetchJob?.cancel()
        feeMinutesToWait = null
        minutesToWaitFetchJob = coroutineScope.launch(Dispatchers.IO) {
            feeMinutesToWait = try {
                ergoApiService.getExpectedWaitTime(
                    feeAmount.nanoErgs,
                    feeTxSize
                ).execute().body()?.toInt()
            } catch (t: Throwable) {
                LogUtils.logDebug(
                    this.javaClass.simpleName,
                    "Error requesting wait time for fee",
                    t
                )
                null
            }
            notifyAmountsChanged()
        }
    }

    fun setNewFeeAmount(feeAmount: ErgoAmount, ergoApiService: ApiServiceManager) {
        val newFeeAmount = ErgoAmount(max(feeAmount.nanoErgs, Parameters.MinFee))
        if (newFeeAmount.nanoErgs != this.feeAmount.nanoErgs) {
            this.feeAmount = newFeeAmount
            feeMinutesToWait = null
            calcGrossAmount()
            fetchFeeWaitTime(ergoApiService)
        }
    }

    // called when needed (user goes into fee choose dialog)
    fun fetchSuggestedFeeData(ergoApiService: ApiServiceManager) {
        if (suggestedFeeJob?.isActive != true && suggestedFeeItems.isEmpty()) {
            suggestedFeeJob = coroutineScope.launch(Dispatchers.IO) {
                suggestedFeeItems = emptyList()
                val fetchedItems = ArrayList<SuggestedFee>()
                val listSpeedPairs: List<Pair<ExecutionSpeed, Int>> = listOf(
                    Pair(ExecutionSpeed.Fast, 5), // five minutes: fast
                    Pair(ExecutionSpeed.Medium, 30), // 30 minutes: medium speed
                    Pair(ExecutionSpeed.Slow, 240), // 240 minutes: slow execution
                )

                listSpeedPairs.takeWhile { speedPair ->
                    val suggestedFee = try {
                        ergoApiService.getSuggestedFee(speedPair.second, feeTxSize).execute().body()
                    } catch (t: Throwable) {
                        LogUtils.logDebug(
                            this.javaClass.simpleName,
                            "Error requesting suggested fees",
                            t
                        )
                        null
                    }

                    suggestedFee?.let {
                        fetchedItems.add(
                            SuggestedFee(
                                speedPair.first,
                                speedPair.second,
                                max(it.toLong(), Parameters.MinFee)
                            )
                        )
                    }

                    suggestedFee != null && suggestedFee > Parameters.MinFee
                }
                suggestedFeeItems = fetchedItems

                onNotifySuggestedFees()
            }
        }
    }

    fun getFeeDescriptionLabel(stringProvider: StringProvider): String {
        val feeText = stringProvider.getString(
            STRING_DESC_FEE,
            feeAmount.toStringRoundToDecimals()
        )

        return feeMinutesToWait?.let { feeMinutesToWait ->
            "$feeText " + stringProvider.getString(
                STRING_DESC_FEE_EXECUTION_TIME,
                feeMinutesToWait.coerceAtLeast(2)
            )
        } ?: feeText
    }

    override fun derivedAddressChanged() {
        super.derivedAddressChanged()

        val address = derivedAddress?.publicAddress
        val addressState = address?.let { wallet?.getStateForAddress(it) }
        wallet?.let { wallet ->
            balance = ErgoAmount(
                addressState?.balance ?: wallet.getBalanceForAllAddresses()
            )
            notifyBalanceChanged()
        }
        tokensAvail.clear()
        val tokensList = address?.let { wallet?.getTokensForAddress(address) }
            ?: wallet?.getTokensForAllAddresses()
        tokensList?.forEach { tokensAvail[it.tokenId ?: ""] = it }
        // remove from chosen what's not available
        // toMutableList copies the list, so we don't get a ConcurrentModificationException when
        // removing elements from the HashMap
        tokensChosen.keys.toMutableList().forEach { tokenId ->
            if (!tokensAvail.containsKey(tokenId))
                tokensChosen.remove(tokenId)
        }

        notifyTokensChosenChanged()
    }

    fun switchInputAmountMode(): Boolean {
        return _amountToSend.switchInputAmountMode()
    }

    fun inputAmountChanged(input: String) {
        _amountToSend.inputAmountChanged(input)
        calcGrossAmount()
    }

    fun setAmountToSendErg(erg: ErgoAmount) {
        _amountToSend.setErgAmount(erg)
        calcGrossAmount()
    }

    fun getOtherCurrencyLabel(textProvider: StringProvider): String? {
        val nodeConnector = WalletStateSyncManager.getInstance()
        return if (nodeConnector.hasFiatValue) {
            if (!_amountToSend.inputIsFiat) {
                textProvider.getString(
                    STRING_LABEL_FIAT_AMOUNT,
                    formatFiatToString(
                        amountToSend.toDouble() * nodeConnector.fiatValue.value.toDouble(),
                        nodeConnector.fiatCurrency, textProvider
                    )
                )
            } else {
                textProvider.getString(
                    STRING_LABEL_FIAT_AMOUNT,
                    textProvider.getString(
                        STRING_LABEL_ERG_AMOUNT,
                        amountToSend.toStringRoundToDecimals()
                    )
                )
            }
        } else
            null
    }

    private fun calcGrossAmount() {
        grossAmount = feeAmount + amountToSend
        notifyAmountsChanged()
    }

    fun getMaxPossibleAmountToSend(): ErgoAmount {
        return ErgoAmount(
            max(
                0L,
                (balance.nanoErgs) - (feeAmount.nanoErgs)
            )
        )
    }

    /**
     * actual amount to send is user entered amount or
     * min possible amount in case no amount was entered and token should be sent
     */
    private fun getActualAmountToSendNanoErgs(): Long {
        return if (tokensChosen.isNotEmpty() && amountToSend.isZero())
            Parameters.MinChangeValue
        else
            amountToSend.nanoErgs
    }

    private fun getActualMessageToSend(): String? {
        return if (message.isBlank()) null else message.take(maxMessageLength)
    }

    fun checkCanMakePayment(preferences: PreferencesProvider): CheckCanPayResponse {
        val receiverOk = isValidErgoAddress(receiverAddress)
        val amountOk = getActualAmountToSendNanoErgs() >= Parameters.MinChangeValue
        val tokensOk = tokensChosen.values.none { it.value <= 0 }
        val messageOk = getActualMessageToSend() == null || preferences.sendTxMessages

        return CheckCanPayResponse(
            canPay = receiverOk && amountOk && tokensOk && messageOk,
            receiverError = !receiverOk,
            messageError = !messageOk,
            amountError = !amountOk,
            tokenError = !tokensOk
        )
    }

    private var preparedTransaction: ByteArray? = null
    private var transactionInfo: TransactionInfo? = null
    fun prepareTransactionForSigning(preferences: PreferencesProvider, texts: StringProvider) {
        preparedTransaction = null
        wallet?.let { wallet ->
            notifyUiLocked(true)
            coroutineScope.launch {
                val serializedTx = prepareSerializedTx(preferences, texts)
                notifyUiLocked(false)
                preparedTransaction = serializedTx.serializedTx
                if (serializedTx.success) {
                    transactionInfo = serializedTx.buildTransactionInfo(tokensAvail)
                    notifyHasPreparedTx(transactionInfo!!)
                }
                notifyHasErgoTxResult(serializedTx)
            }
        }
    }

    override fun startPaymentWithMnemonicAsync(
        signingSecrets: SigningSecrets,
        preferences: PreferencesProvider,
        texts: StringProvider,
        db: IAppDatabase,
    ) {
        notifyUiLocked(true)

        coroutineScope.launch {
            val ergoTxResult: SendTransactionResult
            withContext(Dispatchers.IO) {
                val signingResult = ErgoFacade.signSerializedErgoTx(
                    preparedTransaction!!,
                    signingSecrets, getSigningDerivedAddressesIndices(), texts
                )
                signingSecrets.clearMemory()
                preparedTransaction = null
                if (!signingResult.success) {
                    notifyUiLocked(false)
                    notifyHasErgoTxResult(signingResult)
                } else {
                    ergoTxResult = ErgoFacade.sendSignedErgoTx(
                        signingResult.serializedTx!!, preferences, texts
                    )
                    notifyUiLocked(false)
                    transactionSubmitted(
                        ergoTxResult,
                        db,
                        preferences,
                        transactionInfo
                    )
                }
                transactionInfo = null
            }
        }
    }

    override fun startColdWalletPayment(preferences: PreferencesProvider, texts: StringProvider) {
        wallet?.let { wallet ->
            notifyUiLocked(true)
            coroutineScope.launch {
                val serializedTx = prepareSerializedTx(preferences, texts)
                notifyUiLocked(false)
                startColdWalletPaymentPrompt(serializedTx)
            }
        }
    }

    private suspend fun prepareSerializedTx(
        preferences: PreferencesProvider,
        texts: StringProvider
    ): PromptSigningResult {
        val serializedTx = withContext(Dispatchers.IO) {
            val derivedAddresses = getSigningDerivedAddresses()
            ErgoFacade.prepareSerializedErgoTx(
                Address.create(receiverAddress),
                getActualMessageToSend(),
                getActualAmountToSendNanoErgs(),
                tokensChosen.values.toList(),
                feeAmount.nanoErgs,
                derivedAddresses.map { Address.create(it) },
                balance.nanoErgs,
                tokensAvail,
                consolidate = !wallet!!.isReadOnly(),
                preferences, texts
            )
        }
        return serializedTx
    }

    /**
     * @return list of tokens to choose from, that means available on the wallet and not already chosen
     */
    fun getTokensToChooseFrom(): List<WalletToken> {
        return tokensAvail.values.filter {
            !tokensChosen.containsKey(it.tokenId)
        }.sortedBy { it.name?.lowercase() }
    }

    /**
     * called by UI when user wants to add a token
     */
    fun newTokenChosen(tokenId: String) {
        tokensAvail[tokenId]?.let {
            tokensChosen[tokenId] = ErgoToken(tokenId, if (it.isSingularToken()) 1 else 0)
            notifyTokensChosenChanged()
        }
    }

    /**
     * called by UI when user wants to remove a token
     */
    fun removeToken(tokenId: String) {
        val size = tokensChosen.size
        tokensChosen.remove(tokenId)
        if (tokensChosen.size != size) {
            notifyTokensChosenChanged()
        }
    }

    /**
     * called by UI when user changes token amount
     */
    fun setTokenAmount(tokenId: String, amount: TokenAmount) {
        tokensChosen.get(tokenId)?.let {
            tokensChosen.put(tokenId, ErgoToken(it.id, amount.rawValue))
        }
    }

    fun tokenAmountToText(amount: Long, decimals: Int) =
        if (amount > 0)
            TokenAmount(amount, decimals).toStringTrimTrailingZeros()
        else ""

    fun addTokensFromPaymentRequest(tokens: HashMap<String, String>) {
        // at the moment, tokens are the only thing provided by a payment request apart from
        // amount and receiver. New features added in the future should be handled
        // here as well. addPaymentRequestWarning is only called from this method.
        // If this changes, we might need a new callback onPaymentRequestProcessed() to display
        // the warnings in the UI. For the time being, it is sufficient without a dedicated
        // callback because calls to notifyTokensChosenChanged() going on before reaching this here
        // have no impact on showing warnings messages.

        var changed = false
        tokens.forEach { token ->
            val tokenId = token.key
            val amount = token.value

            // we need to check for existence here, QR code might have any String, not an ID
            tokensAvail[tokenId]?.let {
                val amountFromRequest = amount.toTokenAmount(it.decimals)?.rawValue ?: 0
                val amountToUse =
                    if (amountFromRequest == 0L && it.isSingularToken()) 1 else amountFromRequest
                tokensChosen[tokenId] = ErgoToken(tokenId, amountToUse)
                changed = true
                if (amountToUse != amountFromRequest) {
                    addPaymentRequestWarning(
                        STRING_ERROR_REQUEST_TOKEN_AMOUNT,
                        it.name ?: it.tokenId
                    )
                }
            } ?: addPaymentRequestWarning(STRING_ERROR_REQUEST_TOKEN_NOT_FOUND, tokenId)
        }
        if (changed) {
            if (amountToSend.isZero()) {
                addPaymentRequestWarning(STRING_ERROR_REQUEST_TOKEN_BUT_NO_ERG)
            }

            notifyTokensChosenChanged()
        } else if (paymentRequestWarnings.isNotEmpty()) {
            notifyTokensChosenChanged()
        }
    }

    private fun addPaymentRequestWarning(errorId: String, args: String? = null) {
        paymentRequestWarnings.add(PaymentRequestWarning(errorId, args))
    }

    fun getPaymentRequestWarnings(texts: StringProvider): String? {
        val stringWarnings = paymentRequestWarnings.map { warning ->
            warning.arguments?.let { args -> texts.getString(warning.errorCode, args) }
                ?: texts.getString(warning.errorCode)
        }
        // don't show twice
        paymentRequestWarnings.clear()

        return (if (stringWarnings.isEmpty()) null
        else stringWarnings.joinToString("\n"))
    }

    fun qrCodeScanned(
        qrCodeData: String,
        stringProvider: StringProvider,
        navigateToColdWalletSigning: ((signingData: String, walletId: Int) -> Unit),
        navigateToErgoPaySigning: ((ergoPayRequest: String) -> Unit),
        setPaymentRequestDataToUi: ((receiverAddress: String, amount: ErgoAmount?, message: String?) -> Unit),
    ) {
        if (isColdSigningRequestChunk(qrCodeData)) {
            if (wallet?.walletConfig?.isReadOnly() == false)
                navigateToColdWalletSigning.invoke(qrCodeData, wallet!!.walletConfig.id)
            else
                showErrorMessage(stringProvider.getString(STRING_HINT_READONLY_SIGNING_REQUEST))
        } else if (isErgoPaySigningRequest(qrCodeData)) {
            navigateToErgoPaySigning.invoke(
                qrCodeData
            )
        } else {
            val content = parsePaymentRequest(qrCodeData)
            content?.let {
                setPaymentRequestDataToUi.invoke(
                    content.address,
                    content.amount.let { amount -> if (amount.nanoErgs > 0) amount else null },
                    if (content.description.isNotBlank()) content.description else null
                )
                addTokensFromPaymentRequest(content.tokens)
            } ?: showErrorMessage(
                stringProvider.getString(
                    STRING_ERROR_QR_CODE_CONTENT_UNKNOWN
                )
            )
        }
    }

    abstract fun notifyTokensChosenChanged()
    abstract fun notifyAmountsChanged()
    abstract fun notifyBalanceChanged()
    abstract fun showErrorMessage(message: String)
    abstract fun onNotifySuggestedFees()
    abstract fun notifyHasPreparedTx(preparedTx: TransactionInfo)

    data class CheckCanPayResponse(
        val canPay: Boolean,
        val receiverError: Boolean,
        val messageError: Boolean,
        val amountError: Boolean,
        val tokenError: Boolean
    )
}