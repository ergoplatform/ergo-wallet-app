package org.ergoplatform.uilogic.wallet

import kotlinx.coroutines.flow.MutableStateFlow
import org.ergoplatform.appkit.Address
import org.ergoplatform.appkit.MultisigAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletDbProvider
import org.ergoplatform.wallet.isMultisig

abstract class WalletConfigUiLogic {

    var wallet: WalletConfig? = null
        protected set(value) {
            field = value
            onConfigChanged(value)
        }

    val multisigInfoFlow = MutableStateFlow<MultisigAddress?>(null)

    suspend fun initForWallet(walletId: Int, db: WalletDbProvider) {
        wallet = db.loadWalletConfigById(walletId)

        loadMultisigInfo()
    }

    protected suspend fun loadMultisigInfo() {
        if (wallet?.isMultisig() == true) {
            multisigInfoFlow.value = try {
                MultisigAddress.buildFromAddress(Address.create(wallet!!.firstAddress))
            } catch (t: Throwable) {
                null
            }
        }
    }

    suspend fun saveChanges(db: WalletDbProvider, newWalletName: String?): Boolean {
        return wallet?.let {
            val newWalletConfig = WalletConfig(
                it.id,
                if (newWalletName.isNullOrBlank()) it.displayName else newWalletName,
                it.firstAddress,
                it.encryptionType,
                it.secretStorage,
                it.unfoldTokens,
                it.extendedPublicKey,
                it.walletType,
            )

            db.updateWalletConfig(newWalletConfig)
            wallet = newWalletConfig
            true
        } ?: false
    }

    abstract fun onConfigChanged(value: WalletConfig?)
}