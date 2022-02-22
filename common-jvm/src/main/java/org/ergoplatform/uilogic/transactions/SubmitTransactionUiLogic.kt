package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.sendSignedErgoTx
import org.ergoplatform.transactions.*
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.wallet.getDerivedAddress
import org.ergoplatform.wallet.getDerivedAddressEntity
import org.ergoplatform.wallet.getNumOfAddresses
import org.ergoplatform.wallet.getSortedDerivedAddressesList

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
    var signedTxQrCodePagesCollector: QrCodePagesCollector? = null
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
        mnemonic: String,
        preferences: PreferencesProvider,
        texts: StringProvider
    )

    abstract fun startColdWalletPayment(preferences: PreferencesProvider, texts: StringProvider)

    fun getSigningDerivedAddressesIndices(): List<Int> {
        return derivedAddressIdx?.let { listOf(it) }
            ?: wallet?.getSortedDerivedAddressesList()?.map { it.derivationIndex }
            ?: listOf(0)
    }

    fun getSigningDerivedAddresses(): List<String> {
        return derivedAddressIdx?.let { listOf(wallet!!.getDerivedAddress(it)!!) }
            ?: wallet!!.getSortedDerivedAddressesList().map { it.publicAddress }
    }

    fun startColdWalletPaymentPrompt(serializedTx: PromptSigningResult) {
        signedTxQrCodePagesCollector = QrCodePagesCollector(::getColdSignedTxChunk)
        if (serializedTx.success) {
            buildColdSigningRequest(serializedTx)?.let {
                notifyHasSigningPromptData(it)
            }
        }
        notifyHasErgoTxResult(serializedTx)
    }

    fun sendColdWalletSignedTx(
        preferences: PreferencesProvider,
        texts: StringProvider
    ) {
        val qrCodes = signedTxQrCodePagesCollector?.getAllPages()
        signedTxQrCodePagesCollector = null

        if (qrCodes.isNullOrEmpty()) return // should not happen

        notifyUiLocked(true)
        coroutineScope.launch {
            val ergoTxResult: SendTransactionResult
            withContext(Dispatchers.IO) {
                val signingResult = coldSigningResponseFromQrChunks(qrCodes)
                if (signingResult.success) {
                    ergoTxResult = sendSignedErgoTx(
                        signingResult.serializedTx!!,
                        preferences, texts
                    )
                } else {
                    ergoTxResult = SendTransactionResult(false, errorMsg = signingResult.errorMsg)
                }
            }
            notifyUiLocked(false)
            if (ergoTxResult.success) {
                WalletStateSyncManager.getInstance().invalidateCache()
                notifyHasTxId(ergoTxResult.txId!!)
            }
            notifyHasErgoTxResult(ergoTxResult)
        }

    }

    abstract fun notifyWalletStateLoaded()
    abstract fun notifyDerivedAddressChanged()
    abstract fun notifyUiLocked(locked: Boolean)
    abstract fun notifyHasTxId(txId: String)
    abstract fun notifyHasErgoTxResult(txResult: TransactionResult)
    abstract fun notifyHasSigningPromptData(signingPrompt: String)
}