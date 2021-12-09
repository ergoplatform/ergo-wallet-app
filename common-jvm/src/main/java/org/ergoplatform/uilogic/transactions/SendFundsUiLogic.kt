package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.*
import org.ergoplatform.android.transactions.buildColdSigningRequest
import org.ergoplatform.android.transactions.coldSigningResponseFromQrChunks
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.ErgoToken
import org.ergoplatform.appkit.Parameters
import org.ergoplatform.persistance.*
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.SendTransactionResult
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.wallet.*
import kotlin.math.max

abstract class SendFundsUiLogic {
    abstract val coroutineScope: CoroutineScope

    var wallet: Wallet? = null
        private set

    var derivedAddressIdx: Int? = null
        set(value) {
            field = value
            derivedAddressChanged()
        }
    var derivedAddress: WalletAddress? = null
        private set

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
            content = paymentRequest?.let { parsePaymentRequestFromQuery(paymentRequest) }
            content?.let {
                receiverAddress = content.address
                amountToSend = content.amount
            }
        } else content = null

        coroutineScope.launch {
            wallet = database.loadWalletWithStateById(walletId)

            wallet?.let {
                notifyWalletStateLoaded()
            }

            // no address set (yet)?
            if (derivedAddressIdx == null && firstInit) {
                // if there is only a single address available, fix it to this one
                if (wallet?.getNumOfAddresses() == 1) {
                    derivedAddressIdx = 0
                } else {
                    // make sure to post to observer the first time
                    derivedAddressIdx = if (derivationIdx >= 0) derivationIdx else null
                }
            }

            content?.let {
                addTokensFromQr(content.tokens)
                notifyTokensChosenChanged()
            }
        }
        calcGrossAmount()
    }

    private fun derivedAddressChanged() {
        val addressDbEntity = derivedAddressIdx?.let { wallet?.getDerivedAddressEntity(it) }
        val address = addressDbEntity?.publicAddress
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

        derivedAddress = addressDbEntity
        notifyDerivedAddressChanged()
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
        val userEnteredNanoErgs = amountToSend.nanoErgs
        return if (tokensChosen.isNotEmpty() && userEnteredNanoErgs == 0L)
            Parameters.MinChangeValue
        else
            userEnteredNanoErgs
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

    fun startPaymentWithMnemonicAsync(mnemonic: String, preferences: PreferencesProvider) {
        val derivedAddresses =
            derivedAddressIdx?.let { listOf(it) }
                ?: wallet?.getSortedDerivedAddressesList()?.map { it.derivationIndex }
                ?: listOf(0)

        coroutineScope.launch {
            val ergoTxResult: SendTransactionResult
            withContext(Dispatchers.IO) {
                ergoTxResult = sendErgoTx(
                    Address.create(receiverAddress), getActualAmountToSendNanoErgs(),
                    tokensChosen.values.toList(),
                    mnemonic, "", derivedAddresses,
                    preferences.prefNodeUrl, preferences.prefExplorerApiUrl
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

    fun startColdWalletPayment(preferences: PreferencesProvider) {
        wallet?.let { wallet ->
            val derivedAddresses =
                derivedAddressIdx?.let { listOf(wallet.getDerivedAddress(it)!!) }
                    ?: wallet.getSortedDerivedAddressesList().map { it.publicAddress }

            notifyUiLocked(true)
            coroutineScope.launch {
                val serializedTx: PromptSigningResult
                withContext(Dispatchers.IO) {
                    serializedTx = prepareSerializedErgoTx(
                        Address.create(receiverAddress), getActualAmountToSendNanoErgs(),
                        tokensChosen.values.toList(),
                        derivedAddresses.map { Address.create(it) },
                        preferences.prefNodeUrl, preferences.prefExplorerApiUrl
                    )
                }
                notifyUiLocked(false)
                if (serializedTx.success) {
                    notifyHasSigningPromptData(buildColdSigningRequest(serializedTx))
                }
                notifyHasErgoTxResult(serializedTx)
            }
        }
    }

    fun sendColdWalletSignedTx(qrCodes: List<String>, preferences: PreferencesProvider) {
        notifyUiLocked(true)
        coroutineScope.launch {
            val ergoTxResult: SendTransactionResult
            withContext(Dispatchers.IO) {
                val signingResult = coldSigningResponseFromQrChunks(qrCodes)
                if (signingResult.success) {
                    ergoTxResult = sendSignedErgoTx(
                        signingResult.serializedTx!!,
                        preferences.prefNodeUrl, preferences.prefExplorerApiUrl
                    )
                } else {
                    ergoTxResult = SendTransactionResult(false, errorMsg = signingResult.errorMsg)
                }
            }
            notifyUiLocked(false)
            if (ergoTxResult.success) {
                NodeConnector.getInstance().invalidateCache()
                notifyHasTxId(ergoTxResult.txId!!)
            }
            notifyHasErgoTxResult(ergoTxResult)
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

    fun newTokenChoosen(tokenId: String) {
        tokensChosen.put(tokenId, ErgoToken(tokenId, 0))
        notifyTokensChosenChanged()
    }

    fun removeToken(tokenId: String) {
        val size = tokensChosen.size
        tokensChosen.remove(tokenId)
        if (tokensChosen.size != size) {
            notifyTokensChosenChanged()
        }
    }

    fun setTokenAmount(tokenId: String, amount: TokenAmount) {
        tokensChosen.get(tokenId)?.let {
            tokensChosen.put(tokenId, ErgoToken(it.id, amount.rawValue))
        }
    }

    fun tokenAmountToText(amount: Long, decimals: Int) =
        if (amount > 0)
            TokenAmount(amount, decimals).toStringTrimTrailingZeros()
        else ""

    fun addTokensFromQr(tokens: HashMap<String, String>) {
        var changed = false
        tokens.forEach {
            val tokenId = it.key
            val amount = it.value

            // we need to check for existence here, QR code might have any String, not an ID
            tokensAvail.filter { it.tokenId.equals(tokenId) }.firstOrNull()?.let {
                val longAmount = amount.toTokenAmount(it.decimals)?.rawValue ?: 0
                tokensChosen.put(tokenId, ErgoToken(tokenId, longAmount))
                changed = true
            }
        }
        if (changed) {
            notifyTokensChosenChanged()
        }
    }

    abstract fun notifyWalletStateLoaded()
    abstract fun notifyDerivedAddressChanged()
    abstract fun notifyTokensChosenChanged()
    abstract fun notifyAmountsChanged()
    abstract fun notifyBalanceChanged()
    abstract fun notifyUiLocked(locked: Boolean)
    abstract fun notifyHasTxId(txId: String)
    abstract fun notifyHasErgoTxResult(txResult: TransactionResult)
    abstract fun notifyHasSigningPromptData(signingPrompt: String?)

    data class CheckCanPayResponse(
        val canPay: Boolean,
        val receiverError: Boolean,
        val amountError: Boolean,
        val tokenError: Boolean
    )
}