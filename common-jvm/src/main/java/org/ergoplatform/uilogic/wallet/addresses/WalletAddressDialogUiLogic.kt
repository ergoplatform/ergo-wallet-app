package org.ergoplatform.uilogic.wallet.addresses

import org.ergoplatform.persistance.WalletDbProvider

class WalletAddressDialogUiLogic() {
    suspend fun deleteWalletAddress(database: WalletDbProvider, addrId: Long) {
        database.withTransaction {
            val walletAddress = database.loadWalletAddress(addrId)
            walletAddress?.publicAddress?.let {
                database.deleteAddressState(it)
                database.deleteTokensByAddress(it)
            }
            database.deleteWalletAddress(addrId)
        }
    }

    suspend fun saveWalletAddressLabel(database: WalletDbProvider, addrId: Long, label: String?) {
        database.updateWalletAddressLabel(addrId, if (label.isNullOrBlank()) null else label)
    }
}