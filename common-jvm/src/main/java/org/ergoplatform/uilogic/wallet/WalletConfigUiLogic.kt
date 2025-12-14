package org.ergoplatform.uilogic.wallet

import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.persistance.WalletDbProvider

abstract class WalletConfigUiLogic {

    var wallet: WalletConfig? = null
        protected set(value) {
            field = value
            onConfigChanged(value)
        }

    suspend fun initForWallet(walletId: Int, db: WalletDbProvider) {
        wallet = db.loadWalletConfigById(walletId)
    }

    suspend fun saveChanges(db: WalletDbProvider, newWalletName: String?, newHideBalance: Boolean? = null): Boolean {
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
                newHideBalance ?: it.hideBalance
            )

            db.updateWalletConfig(newWalletConfig)
            wallet = newWalletConfig
            true
        } ?: false
    }

    abstract fun onConfigChanged(value: WalletConfig?)
}