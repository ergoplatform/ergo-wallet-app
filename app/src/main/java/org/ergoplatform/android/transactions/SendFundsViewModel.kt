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
import org.ergoplatform.android.ui.PasswordDialogFragment
import org.ergoplatform.android.ui.SingleLiveEvent
import org.ergoplatform.android.wallet.ENC_TYPE_DEVICE
import org.ergoplatform.android.wallet.ENC_TYPE_PASSWORD
import org.ergoplatform.android.wallet.WalletConfigDbEntity
import org.ergoplatform.android.wallet.WalletTokenDbEntity
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.ErgoToken
import org.ergoplatform.appkit.Parameters
import kotlin.math.pow

/**
 * Holding state of the send funds screen (thus to be expected to get complicated)
 */
class SendFundsViewModel : ViewModel() {
    var wallet: WalletConfigDbEntity? = null
        private set

    var receiverAddress: String = ""
        set(value) {
            field = value
            calcGrossAmount()
        }
    var amountToSend: Double = 0.0
        set(value) {
            field = value
            calcGrossAmount()
        }

    private val _lockInterface = MutableLiveData<Boolean>()
    val lockInterface: LiveData<Boolean> = _lockInterface
    private val _walletName = MutableLiveData<String>()
    val walletName: LiveData<String> = _walletName
    private val _walletBalance = MutableLiveData<Double>()
    val walletBalance: LiveData<Double> = _walletBalance
    private val _feeAmount = MutableLiveData<Double>().apply {
        value = nanoErgsToErgs(Parameters.MinFee)
    }
    val feeAmount: LiveData<Double> = _feeAmount
    private val _grossAmount = MutableLiveData<Double>().apply {
        value = 0.0
    }
    val grossAmount: LiveData<Double> = _grossAmount
    private val _paymentDoneLiveData = SingleLiveEvent<TransactionResult>()
    val paymentDoneLiveData: LiveData<TransactionResult> = _paymentDoneLiveData
    private val _txId = MutableLiveData<String>()
    val txId: LiveData<String> = _txId

    val tokensAvail: ArrayList<WalletTokenDbEntity> = ArrayList()
    val tokensChosen: HashMap<String, ErgoToken> = HashMap()

    // the live data gets data posted on adding or removing tokens, not on every amount change
    private val _tokensChosenLiveData = MutableLiveData<List<String>>()
    val tokensChosenLiveData: LiveData<List<String>> = _tokensChosenLiveData

    fun initWallet(ctx: Context, walletId: Int, paymentRequest: String?) {
        val firstInit = wallet == null

        // on first init, we read an send payment request. Don't do it again on device rotation
        // to not mess with user inputs
        val content: QrCodeContent?
        if (firstInit) {
            content = paymentRequest?.let { parseContentFromQuery(paymentRequest) }
            content?.let {
                receiverAddress = content.address
                amountToSend = content.amount
            }
        } else content = null

        viewModelScope.launch {
            val walletWithState =
                AppDatabase.getInstance(ctx).walletDao().loadWalletWithStateById(walletId)
            wallet = walletWithState?.walletConfig

            wallet?.displayName?.let {
                _walletName.postValue(it)
            }
            walletWithState?.state?.map { it.balance ?: 0 }?.sum()
                ?.let { _walletBalance.postValue(nanoErgsToErgs(it)) }
            tokensAvail.clear()
            walletWithState?.tokens?.let { tokensAvail.addAll(it) }
            content?.let { addTokensFromQr(content.tokens) }
            notifyTokensChosenChanged()
        }
        calcGrossAmount()
    }

    private fun calcGrossAmount() {
        _grossAmount.postValue(feeAmount.value!! + amountToSend)
    }

    fun checkReceiverAddress(): Boolean {
        return isValidErgoAddress(receiverAddress)
    }

    fun checkAmount(): Boolean {
        return amountToSend >= nanoErgsToErgs(Parameters.MinChangeValue)
    }

    fun checkTokens(): Boolean {
        return tokensChosen.values.filter { it.value <= 0 }.isEmpty()
    }

    fun preparePayment(fragment: SendFundsFragmentDialog) {
        if (wallet?.encryptionType == ENC_TYPE_PASSWORD) {
            PasswordDialogFragment().show(
                fragment.childFragmentManager,
                null
            )
        } else if (wallet?.encryptionType == ENC_TYPE_DEVICE) {
            fragment.showBiometricPrompt()
        }
    }

    fun startPaymentWithPassword(password: String, context: Context): Boolean {
        wallet?.secretStorage?.let {
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

            startPaymentWithMnemonicAsync(mnemonic, context)

            _lockInterface.postValue(true)

            return true
        }

        return false
    }

    fun startPaymentUserAuth(context: Context) {
        // we don't handle exceptions here by intention: we throw them back to the fragment which
        // will show a snackbar to give the user a hint what went wrong
        wallet?.secretStorage?.let {
            val mnemonic: String?

            val decryptData = AesEncryptionManager.decryptDataWithDeviceKey(it)
            mnemonic = deserializeSecrets(String(decryptData!!))

            startPaymentWithMnemonicAsync(mnemonic!!, context)

            _lockInterface.postValue(true)

        }
    }

    private fun startPaymentWithMnemonicAsync(mnemonic: String, context: Context) {
        viewModelScope.launch {
            val ergoTxResult: TransactionResult
            withContext(Dispatchers.IO) {
                ergoTxResult = sendErgoTx(
                    Address.create(receiverAddress), ergsToNanoErgs(amountToSend),
                    tokensChosen.values.toList(),
                    mnemonic, "", 0,
                    getPrefNodeUrl(context), getPrefExplorerApiUrl(context)
                )
            }
            _lockInterface.postValue(false)
            if (ergoTxResult.success) {
                NodeConnector.getInstance().invalidateCache()
                _txId.postValue(ergoTxResult.txId!!)
            }
            _paymentDoneLiveData.postValue(ergoTxResult)
        }
    }

    /**
     * @return list of tokens to choose from, that means available on the wallet and not already chosen
     */
    fun getTokensToChooseFrom(): List<WalletTokenDbEntity> {
        return tokensAvail.filter {
            !tokensChosen.containsKey(it.tokenId)
        }
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

    fun setTokenAmount(tokenId: String, amount: Long) {
        tokensChosen.get(tokenId)?.let {
            tokensChosen.put(tokenId, ErgoToken(it.id, amount))
        }
    }

    fun addTokensFromQr(tokens: HashMap<String, Double>) {
        var changed = false
        tokens.forEach {
            val tokenId = it.key
            val amount = it.value

            // we need to check for existence here, QR code might have any String, not an ID
            tokensAvail.filter { it.tokenId.equals(tokenId) }.firstOrNull()?.let {
                val longAmount = (amount * 10.0.pow(it.decimals ?: 0)).toLong()
                tokensChosen.put(tokenId, ErgoToken(tokenId, longAmount))
                changed = true
            }
        }
        if (changed) {
            notifyTokensChosenChanged()
        }
    }

    private fun notifyTokensChosenChanged() {
        _tokensChosenLiveData.postValue(tokensChosen.keys.toList())
    }
}