package org.ergoplatform.android.transactions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.wallet.WalletDbEntity
import org.ergoplatform.explorer.client.model.TransactionInfo

class ColdWalletSigningViewModel : ViewModel() {

    var pagesQrCode = 0
    val pagesAdded get() = qrCodeChunks.size
    val qrCodeChunks = HashMap<Int, String>()
    private val _reducedTx = MutableLiveData<TransactionInfo?>()
    val reducedTx: LiveData<TransactionInfo?> = _reducedTx
    var wallet: WalletDbEntity? = null

    fun addQrCodeChunk(qrCodeChunk: String): Boolean {

        // qr code not fitting, no qr code chunk, or we are already done? => don't add
        if (reducedTx.value != null || !isColdSigningRequestChunk(qrCodeChunk)) {
            return false
        }

        val page = getColdSigingRequestChunkIndex(qrCodeChunk)
        val count = getColdSigingRequestChunkPagesCount(qrCodeChunk)

        if (pagesQrCode != 0 && count != pagesQrCode) {
            return false
        }

        qrCodeChunks.put(page, qrCodeChunk)
        pagesQrCode = count

        buildRequestWhenApplicable()

        return true
    }

    private fun buildRequestWhenApplicable() {
        if (pagesAdded == pagesQrCode) {
            try {
                val signingRequest = coldSigningRequestFromQrChunks(qrCodeChunks.values)
                _reducedTx.postValue(buildTransactionInfoFromReduced(signingRequest.serializedTx!!, signingRequest.serializedInputs))
            } catch (t: Throwable) {

            }
        }
    }

    fun setWalletId(walletId: Int, ctx: Context) {
        viewModelScope.launch {
            wallet =
                AppDatabase.getInstance(ctx).walletDao().loadWalletWithStateById(walletId)
        }
    }
}