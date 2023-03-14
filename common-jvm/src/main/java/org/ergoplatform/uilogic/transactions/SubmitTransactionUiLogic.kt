package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.ErgoFacade
import org.ergoplatform.SigningSecrets
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.persistance.*
import org.ergoplatform.transactions.*
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.wallet.*

abstract class SubmitTransactionUiLogic {
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
    var signingPromptDialogConfig: SigningPromptDialogDataSource? = null
        private set
    var multisigTransactionId: String? = null
        private set

    protected suspend fun initWallet(
        database: WalletDbProvider,
        walletId: Int,
        derivationIdx: Int
    ) {
        val firstInit = wallet == null
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
    }

    protected open fun derivedAddressChanged() {
        derivedAddress = derivedAddressIdx?.let { wallet?.getDerivedAddressEntity(it) }
        notifyDerivedAddressChanged()
    }

    abstract fun startPaymentWithMnemonicAsync(
        signingSecrets: SigningSecrets,
        preferences: PreferencesProvider,
        texts: StringProvider,
        db: IAppDatabase,
    )

    /**
     * Starts a payment for a read only or multisig wallet
     */
    abstract fun startColdWalletOrMultisigPayment(
        preferences: PreferencesProvider,
        texts: StringProvider,
        transactionDbProvider: TransactionDbProvider,
    )

    fun getSigningDerivedAddressesIndices(): List<Int> {
        return derivedAddressIdx?.let { listOf(it) }
            ?: wallet?.getSortedDerivedAddressesList()?.map { it.derivationIndex }
            ?: listOf(0)
    }

    fun getSigningDerivedAddresses(): List<String> {
        return derivedAddressIdx?.let { listOf(wallet!!.getDerivedAddress(it)!!) }
            ?: wallet!!.getSortedDerivedAddressesList().map { it.publicAddress }
    }

    /**
     * starts the payment prompt for multisig or cold wallet
     */
    suspend fun startMultisigOrColdWalletPaymentPrompt(
        serializedTx: PromptSigningResult,
        transactionDbProvider: TransactionDbProvider,
    ) {
        if (serializedTx.success) {
            if (wallet!!.isMultisig()) {
                val data =
                    MultisigUtils.createMultisigTransaction(wallet!!.walletConfig, serializedTx)
                transactionDbProvider.insertOrUpdateMultisigTransaction(data)
                multisigTransactionId = data.txId
            } else
                buildColdSigningRequest(serializedTx)?.let {
                    signingPromptDialogConfig = SigningPromptConfig(it)
                    notifyHasSigningPromptData(it)
                }
        }
        notifyHasErgoTxResult(serializedTx)
    }

    fun sendColdWalletSignedTx(
        preferences: PreferencesProvider,
        texts: StringProvider,
        db: IAppDatabase,
    ) {
        val qrCodes = signingPromptDialogConfig?.responsePagesCollector?.getAllPages()
        signingPromptDialogConfig = null

        if (qrCodes.isNullOrEmpty()) return // should not happen

        notifyUiLocked(true)
        coroutineScope.launch {
            val ergoTxResult: SendTransactionResult
            withContext(Dispatchers.IO) {
                val signingResult = coldSigningResponseFromQrChunks(qrCodes)
                if (signingResult.success) {
                    ergoTxResult = ErgoFacade.sendSignedErgoTx(
                        signingResult.serializedTx!!,
                        preferences, texts
                    )
                } else {
                    ergoTxResult = SendTransactionResult(false, errorMsg = signingResult.errorMsg)
                }
                notifyUiLocked(false)
                transactionSubmitted(ergoTxResult, db.transactionDbProvider, preferences)
            }
        }

    }

    protected suspend fun transactionSubmitted(
        ergoTxResult: SendTransactionResult,
        db: TransactionDbProvider,
        preferences: PreferencesProvider,
        transactionInfo: TransactionInfo? = null
    ) {
        if (ergoTxResult.success) {
            try {
                // save submitted transaction to every address
                val txInfoToSave = (transactionInfo
                    ?: ergoTxResult.sentTransaction?.buildTransactionInfo(
                        ApiServiceManager.getOrInit(preferences),
                        preferences
                    ))

                txInfoToSave?.let {
                    getSigningDerivedAddresses().forEach { address ->
                        TransactionListManager.convertAndSaveTransactionInfoToDb(
                            txInfoToSave,
                            address,
                            System.currentTimeMillis(),
                            INCLUSION_HEIGHT_UNCONFIRMED,
                            TX_STATE_SUBMITTED,
                            db
                        )
                    }
                }

            } catch (t: Throwable) {
                // ignore, don't save submitted tx
            }
            WalletStateSyncManager.getInstance().invalidateCache()
            notifyHasSubmittedTxId(ergoTxResult.txId!!)
        }
        notifyHasErgoTxResult(ergoTxResult)
    }

    abstract fun notifyWalletStateLoaded()
    abstract fun notifyDerivedAddressChanged()
    abstract fun notifyUiLocked(locked: Boolean)
    abstract fun notifyHasSubmittedTxId(txId: String)
    abstract fun notifyHasErgoTxResult(txResult: TransactionResult)
    abstract fun notifyHasSigningPromptData(signingPrompt: String)

    private inner class SigningPromptConfig(override val signingPromptData: String) :
        SigningPromptDialogDataSource {

        override val responsePagesCollector: QrCodePagesCollector =
            QrCodePagesCollector(::getColdSignedTxChunk)

        override fun signingRequestToQrChunks(
            serializedSigningRequest: String,
            sizeLimit: Int
        ): List<String> = coldSigningRequestToQrChunks(serializedSigningRequest, sizeLimit)
    }
}