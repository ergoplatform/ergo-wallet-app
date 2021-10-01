package org.ergoplatform.android.transactions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.android.*
import org.ergoplatform.android.wallet.WalletDbEntity
import org.ergoplatform.android.wallet.getSortedDerivedAddressesList
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.explorer.client.model.TransactionInfo
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.SigningResult

class ColdWalletSigningViewModel : ViewModel() {

    var pagesQrCode = 0
    val pagesAdded get() = qrCodeChunks.size
    val qrCodeChunks = HashMap<Int, String>()
    var signedQrCode: List<String>? = null
        private set
    private var signingRequest: PromptSigningResult? = null
    private val _transactionInfo = MutableLiveData<TransactionInfo?>()
    val transactionInfo: LiveData<TransactionInfo?> = _transactionInfo
    private val _lockInterface = MutableLiveData<Boolean>()
    val lockInterface: LiveData<Boolean> = _lockInterface
    private val _signingResult = MutableLiveData<SigningResult?>()
    val signingResult: LiveData<SigningResult?> = _signingResult
    var wallet: WalletDbEntity? = null

    fun addQrCodeChunk(qrCodeChunk: String): Boolean {

        // qr code not fitting, no qr code chunk, or we are already done? => don't add
        if (transactionInfo.value != null || !isColdSigningRequestChunk(qrCodeChunk)) {
            return false
        }

        val page = getQrChunkIndex(qrCodeChunk)
        val count = getQrChunkPagesCount(qrCodeChunk)

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
                val sr = coldSigningRequestFromQrChunks(qrCodeChunks.values)
                signingRequest = sr
                _transactionInfo.postValue(
                    buildTransactionInfoFromReduced(
                        sr.serializedTx!!,
                        sr.serializedInputs
                    )
                )
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

    fun signTxWithPassword(password: String, context: Context): Boolean {
        wallet?.walletConfig?.secretStorage?.let {
            val mnemonic: String?
            try {
                val decryptData = AesEncryptionManager.decryptData(password, it)
                mnemonic = deserializeSecrets(String(decryptData!!))
            } catch (t: Throwable) {
                // Password wrong
                return false
            }

            if (mnemonic == null) {
                // deserialization error, corrupted db data
                return false
            }

            signTxWithMnemonicAsync(mnemonic, context)

            return true
        }

        return false
    }

    fun signTxUserAuth(context: Context) {
        // we don't handle exceptions here by intention: we throw them back to the caller which
        // will show a snackbar to give the user a hint what went wrong
        wallet?.walletConfig?.secretStorage?.let {
            val mnemonic: String?

            val decryptData = AesEncryptionManager.decryptDataWithDeviceKey(it)
            mnemonic = deserializeSecrets(String(decryptData!!))

            signTxWithMnemonicAsync(mnemonic!!, context)

        }
    }

    private fun signTxWithMnemonicAsync(mnemonic: String, context: Context) {
        signingRequest?.let { signingRequest ->
            val derivedAddresses =
                wallet!!.getSortedDerivedAddressesList().map { it.derivationIndex }

            viewModelScope.launch {
                val ergoTxResult: SigningResult
                withContext(Dispatchers.IO) {
                    ergoTxResult = signSerializedErgoTx(
                        signingRequest.serializedTx!!, mnemonic, "",
                        derivedAddresses
                    )
                    signedQrCode = buildColdSigningResponse(ergoTxResult)?.let {
                        coldSigningResponseToQrChunks(
                            it,
                            QR_SIZE_LIMIT
                        )
                    }
                }
                _lockInterface.postValue(false)

                _signingResult.postValue(ergoTxResult)
            }

            _lockInterface.postValue(true)
        }
    }
}