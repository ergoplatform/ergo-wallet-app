package org.ergoplatform.android.wallet

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.deserializeSecrets
import org.ergoplatform.android.getPublicErgoAddressFromMnemonic
import org.ergoplatform.api.AesEncryptionManager

class WalletAddressesViewModel : ViewModel() {
    var wallet: WalletDbEntity? = null
        private set

    private val _addresses = MutableLiveData<List<WalletAddressDbEntity>>()
    val addresses: LiveData<List<WalletAddressDbEntity>> = _addresses

    fun init(ctx: Context, walletId: Int) {
        viewModelScope.launch {
            AppDatabase.getInstance(ctx).walletDao().walletWithStateByIdAsFlow(walletId).collect {
                // called every time something changes in the DB
                wallet = it

                _addresses.postValue(it.getSortedDerivedAddressesList())
            }
        }
    }

    private fun addNextAddresses(ctx: Context, number: Int, mnemonic: String) {
        viewModelScope.launch {
            val sortedAddresses = addresses.value
            sortedAddresses?.let {
                val walletDao = AppDatabase.getInstance(ctx).walletDao()

                var nextIdx = 0

                // find next free slot
                val indices = sortedAddresses.map { it.derivationIndex }

                for (i in 1..number) {
                    while (indices.contains(nextIdx)) {
                        nextIdx++
                    }

                    // okay, we have the next address idx - now get the address
                    val nextAddress = getPublicErgoAddressFromMnemonic(mnemonic, nextIdx)

                    walletDao.insertWalletAddress(
                        WalletAddressDbEntity(
                            0, wallet!!.walletConfig.firstAddress!!, nextIdx,
                            nextAddress, null
                        )
                    )

                    // TODO make nodeconnector fetch the balance
                }
            }
        }
    }

    fun addAddressWithBiometricAuth(ctx: Context, num: Int) {
        wallet?.walletConfig?.secretStorage?.let {
            val decryptData = AesEncryptionManager.decryptDataWithDeviceKey(it)
            deserializeSecrets(String(decryptData!!))?.let { mnemonic ->
                addNextAddresses(ctx, num, mnemonic)
            }
        }
    }

    fun addAddressWithPass(ctx: Context, password: String, num: Int): Boolean {
        wallet?.walletConfig?.secretStorage?.let {
            try {
                val decryptData = AesEncryptionManager.decryptData(password, it)
                deserializeSecrets(String(decryptData!!))?.let { mnemonic ->
                    addNextAddresses(ctx, num, mnemonic)
                    return true
                }
            } catch (t: Throwable) {
                // Password wrong
            }

        }
        return false
    }
}