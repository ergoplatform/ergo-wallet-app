package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.*
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.ErgoToken
import org.ergoplatform.appkit.Parameters
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.tokens.isSingularToken
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.SendTransactionResult
import org.ergoplatform.transactions.isColdSigningRequestChunk
import org.ergoplatform.transactions.isErgoPaySigningRequest
import org.ergoplatform.uilogic.*
import org.ergoplatform.wallet.getBalanceForAllAddresses
import org.ergoplatform.wallet.getStateForAddress
import org.ergoplatform.wallet.getTokensForAddress
import org.ergoplatform.wallet.getTokensForAllAddresses
import kotlin.math.max

abstract class SendFundsUiLogic : SubmitTransactionUiLogic() {

    var receiverAddress: String = ""
        set(value) {
            field = value
            calcGrossAmount()
        }

    /**
     * amount to send, entered by user
     */
    var amountToSend: ErgoAmount = ErgoAmount.ZERO
        set(value) {
            field = value
            calcGrossAmount()
        }

    var feeAmount: ErgoAmount = ErgoAmount(Parameters.MinFee)
        private set
    var grossAmount: ErgoAmount = ErgoAmount.ZERO
        private set
    var balance: ErgoAmount = ErgoAmount.ZERO
        private set

    val tokensAvail: ArrayList<WalletToken> = ArrayList()
    val tokensChosen: HashMap<String, ErgoToken> = HashMap()

    private val paymentRequestWarnings = ArrayList<PaymentRequestWarning>()

    fun initWallet(
        database: WalletDbProvider,
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
                amountToSend = content.amount
            }
        } else content = null

        coroutineScope.launch {
            initWallet(database, walletId, derivationIdx)

            content?.let {
                addTokensFromPaymentRequest(content.tokens)
                notifyTokensChosenChanged()
            }
        }
        calcGrossAmount()
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
        tokensList?.let { tokensAvail.addAll(it) }
        // remove from chosen what's not available
        // toMutableList copies the list, so we don't get a ConcurrentModificationException when
        // removing elements from the HashMap
        tokensChosen.keys.toMutableList().forEach { tokenId ->
            if (tokensAvail.find { it.tokenId.equals(tokenId) } == null)
                tokensChosen.remove(tokenId)
        }

        notifyTokensChosenChanged()
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

    fun checkCanMakePayment(): CheckCanPayResponse {
        val receiverOk = isValidErgoAddress(receiverAddress)
        val amountOk = getActualAmountToSendNanoErgs() >= Parameters.MinChangeValue
        val tokensOk = tokensChosen.values.none { it.value <= 0 }

        return CheckCanPayResponse(
            receiverOk && amountOk && tokensOk,
            !receiverOk,
            !amountOk,
            !tokensOk
        )
    }

    override fun startPaymentWithMnemonicAsync(
        mnemonic: String,
        preferences: PreferencesProvider,
        texts: StringProvider
    ) {
        val derivedAddresses = getSigningDerivedAddressesIndices()

        coroutineScope.launch {
            val ergoTxResult: SendTransactionResult
            withContext(Dispatchers.IO) {
                ergoTxResult = sendErgoTx(
                    Address.create(receiverAddress), getActualAmountToSendNanoErgs(),
                    tokensChosen.values.toList(),
                    mnemonic, "", derivedAddresses,
                    preferences, texts
                )
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

    override fun startColdWalletPayment(preferences: PreferencesProvider, texts: StringProvider) {
        wallet?.let { wallet ->
            val derivedAddresses = getSigningDerivedAddresses()

            notifyUiLocked(true)
            coroutineScope.launch {
                val serializedTx: PromptSigningResult
                withContext(Dispatchers.IO) {
                    serializedTx = prepareSerializedErgoTx(
                        Address.create(receiverAddress), getActualAmountToSendNanoErgs(),
                        tokensChosen.values.toList(),
                        derivedAddresses.map { Address.create(it) },
                        preferences, texts
                    )
                }
                notifyUiLocked(false)
                startColdWalletPaymentPrompt(serializedTx)
            }
        }
    }

    /**
     * @return list of tokens to choose from, that means available on the wallet and not already chosen
     */
    fun getTokensToChooseFrom(): List<WalletToken> {
        return tokensAvail.filter {
            !tokensChosen.containsKey(it.tokenId)
        }.sortedBy { it.name?.lowercase() }
    }

    /**
     * called by UI when user wants to add a token
     */
    fun newTokenChosen(tokenId: String) {
        tokensAvail.firstOrNull { it.tokenId.equals(tokenId) }?.let {
            tokensChosen.put(tokenId, ErgoToken(tokenId, if (it.isSingularToken()) 1 else 0))
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
        tokens.forEach {
            val tokenId = it.key
            val amount = it.value

            // we need to check for existence here, QR code might have any String, not an ID
            tokensAvail.firstOrNull { it.tokenId.equals(tokenId) }?.let {
                val amountFromRequest = amount.toTokenAmount(it.decimals)?.rawValue ?: 0
                val amountToUse =
                    if (amountFromRequest == 0L && it.isSingularToken()) 1 else amountFromRequest
                tokensChosen.put(tokenId, ErgoToken(tokenId, amountToUse))
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
        setPaymentRequestDataToUi: ((receiverAddress: String, amount: ErgoAmount?) -> Unit),
    ) {
        if (wallet?.walletConfig?.secretStorage != null && isColdSigningRequestChunk(qrCodeData)) {
            navigateToColdWalletSigning.invoke(qrCodeData, wallet!!.walletConfig.id)
        } else if (isErgoPaySigningRequest(qrCodeData)) {
            navigateToErgoPaySigning.invoke(
                qrCodeData
            )
        } else {
            val content = parsePaymentRequest(qrCodeData)
            content?.let {
                setPaymentRequestDataToUi.invoke(
                    content.address,
                    content.amount.let { amount -> if (amount.nanoErgs > 0) amount else null }
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

    data class CheckCanPayResponse(
        val canPay: Boolean,
        val receiverError: Boolean,
        val amountError: Boolean,
        val tokenError: Boolean
    )
}