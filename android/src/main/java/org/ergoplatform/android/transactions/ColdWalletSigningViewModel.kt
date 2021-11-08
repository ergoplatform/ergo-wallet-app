package org.ergoplatform.android.transactions

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.deserializeSecrets
import org.ergoplatform.signSerializedErgoTx
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.explorer.client.model.TransactionInfo
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.transactions.PromptSigningResult
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.wallet.getSortedDerivedAddressesList

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
    var wallet: Wallet? = null

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

        val ti = buildRequestWhenApplicable()
        _transactionInfo.postValue(ti)

        return true
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

    fun setWalletId(walletId: Int, ctx: Context) {
        viewModelScope.launch {
            wallet =
                AppDatabase.getInstance(ctx).walletDao().loadWalletWithStateById(walletId)?.toModel()
        }
    }

    fun signTxWithPassword(password: String): Boolean {
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

            signTxWithMnemonicAsync(mnemonic)

            return true
        }

        return false
    }

    fun signTxUserAuth() {
        // we don't handle exceptions here by intention: we throw them back to the caller which
        // will show a snackbar to give the user a hint what went wrong
        wallet?.walletConfig?.secretStorage?.let {
            val mnemonic: String?

            val decryptData = AesEncryptionManager.decryptDataWithDeviceKey(it)
            mnemonic = deserializeSecrets(String(decryptData!!))

            signTxWithMnemonicAsync(mnemonic!!)

        }
    }

    private fun signTxWithMnemonicAsync(mnemonic: String) {
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