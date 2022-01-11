package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.explorer.client.model.TransactionInfo
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.signSerializedErgoTx
import org.ergoplatform.transactions.*
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.wallet.getSortedDerivedAddressesList

abstract class ColdWalletSigningUiLogic {
    abstract val coroutineScope: CoroutineScope

    var state = State.SCANNING
        private set
    var pagesQrCode = 0
    val pagesAdded get() = qrCodeChunks.size
    private val qrCodeChunks = HashMap<Int, String>()
    var signedQrCode: String? = null
        private set
    private var signingRequest: PromptSigningResult? = null

    var wallet: Wallet? = null
        private set

    var transactionInfo: TransactionInfo? = null
        private set
    var lastErrorMessage: String? = null
        private set

    /**
     * Adds QR code chunk when applicable
     *
     * @return TransactionInfo when it could be built, null otherwise. In case no info is built there
     *         might be warnings or error messages available in lastErrorMessage.
     */
    fun addQrCodeChunk(qrCodeChunk: String): TransactionInfo? {

        // are we are already done? => don't add
        if (transactionInfo != null) {
            return transactionInfo
        }

        lastErrorMessage = null

        val qrChunk = getColdSigningRequestChunk(qrCodeChunk)

        // qr code not fitting or no qr code chunk
        if (qrChunk == null) {
            lastErrorMessage = "Not a cold signing QR code"
            return null
        }

        val page = qrChunk.index
        val count = qrChunk.pages

        if (pagesQrCode != 0 && count != pagesQrCode) {
            lastErrorMessage = "QR code does not belong to the formerly scanned codes"
            return null
        }

        qrCodeChunks.put(page, qrCodeChunk)
        pagesQrCode = count

        transactionInfo = buildRequestWhenApplicable()
        transactionInfo?.let { state = State.WAITING_TO_CONFIRM }

        return transactionInfo
    }

    private fun buildRequestWhenApplicable(): TransactionInfo? {
        if (pagesAdded == pagesQrCode) {
            try {
                val sr = coldSigningRequestFromQrChunks(qrCodeChunks.values)
                signingRequest = sr
                return buildTransactionInfoFromReduced(
                    sr.serializedTx!!,
                    sr.serializedInputs
                )
            } catch (t: Throwable) {
                LogUtils.logDebug("ColdWalletSigning", "Error thrown on signing", t)
                val message = t.message ?: t.javaClass.name
                lastErrorMessage = "Error: $message"
            }
        }

        return null
    }

    fun setWalletId(walletId: Int, db: WalletDbProvider) {
        coroutineScope.launch {
            wallet = db.loadWalletWithStateById(walletId)
        }
    }

    fun signTxWithMnemonicAsync(mnemonic: String, texts: StringProvider) {
        signingRequest?.let { signingRequest ->
            val derivedAddresses =
                wallet!!.getSortedDerivedAddressesList().map { it.derivationIndex }

            coroutineScope.launch {
                val ergoTxResult: SigningResult
                withContext(Dispatchers.IO) {
                    ergoTxResult = signSerializedErgoTx(
                        signingRequest.serializedTx!!, mnemonic, "",
                        derivedAddresses, texts
                    )
                    signedQrCode = buildColdSigningResponse(ergoTxResult)
                }
                notifyUiLocked(false)

                if (ergoTxResult.success && signedQrCode != null) {
                    state = State.PRESENT_RESULT
                }
                notifySigningResult(ergoTxResult)
            }

            notifyUiLocked(true)
        }
    }

    abstract fun notifyUiLocked(locked: Boolean)
    abstract fun notifySigningResult(ergoTxResult: SigningResult)

    enum class State { SCANNING, WAITING_TO_CONFIRM, PRESENT_RESULT }
}