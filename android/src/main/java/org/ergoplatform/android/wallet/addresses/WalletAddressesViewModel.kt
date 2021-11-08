package org.ergoplatform.android.wallet.addresses

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.NodeConnector
import org.ergoplatform.android.*
import org.ergoplatform.android.wallet.WalletAddressDbEntity
import org.ergoplatform.api.AesEncryptionManager
import org.ergoplatform.deserializeSecrets
import org.ergoplatform.getPublicErgoAddressFromMnemonic
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.wallet.getSortedDerivedAddressesList

class WalletAddressesViewModel : ViewModel() {
    var numAddressesToAdd: Int = 1
    var wallet: Wallet? = null
        private set

    private val _addresses = MutableLiveData<List<WalletAddress>>()
    val addresses: LiveData<List<WalletAddress>> = _addresses

    private val _lockProgress = MutableLiveData<Boolean>()
    val lockProgress: LiveData<Boolean> = _lockProgress

    fun init(ctx: Context, walletId: Int) {
        viewModelScope.launch {
            AppDatabase.getInstance(ctx).walletDao().walletWithStateByIdAsFlow(walletId).collect {
                // called every time something changes in the DB
                wallet = it?.toModel()
                wallet?.let {
                    _addresses.postValue(it.getSortedDerivedAddressesList())
                }
            }
        }
    }

    private fun addNextAddresses(ctx: Context, number: Int, mnemonic: String) {
        // firing up appkit for the first time needs some time on medium end devices, so do this on
        // background thread while showing infinite progress bar
        viewModelScope.launch(Dispatchers.IO) {
            val sortedAddresses = addresses.value
            sortedAddresses?.let {
                val addedAddresses = mutableListOf<String>()
                val database = AppDatabase.getInstance(ctx)
                val walletDao = database.walletDao()

                var nextIdx = 0

                // find next free slot
                val indices = sortedAddresses.map { it.derivationIndex }.toMutableList()

                _lockProgress.postValue(true)
                database.withTransaction {
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
                        indices.add(nextIdx)
                        addedAddresses.add(nextAddress)
                    }
                }
                _lockProgress.postValue(false)
                // make NodeConnector fetch the balances of the added addresses, in case they
                // were used before
                NodeConnector.getInstance().refreshSingleAddresses(
                    Preferences(ctx),
                    RoomWalletDbProvider(AppDatabase.getInstance(ctx)), addedAddresses
                )
            }
        }
    }

    fun addAddressWithBiometricAuth(ctx: Context) {
        wallet?.walletConfig?.secretStorage?.let {
            val decryptData = AesEncryptionManager.decryptDataWithDeviceKey(it)
            deserializeSecrets(String(decryptData!!))?.let { mnemonic ->
                addNextAddresses(ctx, numAddressesToAdd, mnemonic)
            }
        }
    }

    fun addAddressWithPass(ctx: Context, password: String): Boolean {
        wallet?.walletConfig?.secretStorage?.let {
            try {
                val decryptData = AesEncryptionManager.decryptData(password, it)
                deserializeSecrets(String(decryptData!!))?.let { mnemonic ->
                    addNextAddresses(ctx, numAddressesToAdd, mnemonic)
                    return true
                }
            } catch (t: Throwable) {
                // Password wrong
            }

        }
        return false
    }
}