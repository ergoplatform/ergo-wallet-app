package org.ergoplatform.uilogic.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.android.transactions.*
import org.ergoplatform.explorer.client.model.TransactionInfo
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.signSerializedErgoTx
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.wallet.getSortedDerivedAddressesList

abstract class ColdWalletSigningUiLogic {
    abstract val coroutineScope: CoroutineScope

    var pagesQrCode = 0
    val pagesAdded get() = qrCodeChunks.size
    private val qrCodeChunks = HashMap<Int, String>()
    var signedQrCode: List<String>? = null
        private set
    private var signingRequest: PromptSigningResult? = null

    var wallet: Wallet? = null
        private set

    private var transactionInfo: TransactionInfo? = null

    /**
     * Adds QR code chunk when applicable
     *
     * @return TransactionInfo when it could be built, null otherwise
     */
    fun addQrCodeChunk(qrCodeChunk: String): TransactionInfo? {

        // qr code not fitting, no qr code chunk, or we are already done? => don't add
        if (transactionInfo != null || !isColdSigningRequestChunk(qrCodeChunk)) {
            return transactionInfo
        }

        val page = getQrChunkIndex(qrCodeChunk)
        val count = getQrChunkPagesCount(qrCodeChunk)

        if (pagesQrCode != 0 && count != pagesQrCode) {
            return transactionInfo
        }

        qrCodeChunks.put(page, qrCodeChunk)
        pagesQrCode = count

        transactionInfo = buildRequestWhenApplicable()

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
                    signedQrCode = buildColdSigningResponse(ergoTxResult)?.let {
                        coldSigningResponseToQrChunks(
                            it,
                            QR_SIZE_LIMIT
                        )
                    }
                }
                notifyUiLocked(false)

                notifySigningResult(ergoTxResult)
            }

            notifyUiLocked(true)
        }
    }

    abstract fun notifyUiLocked(locked: Boolean)
    abstract fun notifySigningResult(ergoTxResult: SigningResult)

}