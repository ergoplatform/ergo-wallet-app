package org.ergoplatform.android.transactions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ergoplatform.UnsignedErgoLikeTransaction
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.deserializeUnsignedTx
import org.ergoplatform.android.wallet.WalletDbEntity

class ColdWalletSigningViewModel : ViewModel() {

    var pagesQrCode = 0
    val pagesAdded get() = qrCodeChunks.size
    val qrCodeChunks = HashMap<Int, String>()
    private val _reducedTx = MutableLiveData<UnsignedErgoLikeTransaction?>()
    val reducedTx: LiveData<UnsignedErgoLikeTransaction?> = _reducedTx
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
                _reducedTx.postValue(deserializeUnsignedTx(signingRequest.serializedTx!!))
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