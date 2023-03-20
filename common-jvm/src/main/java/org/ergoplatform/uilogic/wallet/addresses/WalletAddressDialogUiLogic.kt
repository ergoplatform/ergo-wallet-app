package org.ergoplatform.uilogic.wallet.addresses

import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.WalletDbProvider

class WalletAddressDialogUiLogic {
    suspend fun deleteWalletAddress(database: IAppDatabase, addrId: Long) {
        val walletDbProvider = database.walletDbProvider
        walletDbProvider.withTransaction {
            val walletAddress = walletDbProvider.loadWalletAddress(addrId)
            walletAddress?.publicAddress?.let { publicAddress ->
                walletDbProvider.deleteAddressState(publicAddress)
                walletDbProvider.deleteTokensByAddress(publicAddress)
                database.transactionDbProvider.deleteAddressTransactions(publicAddress)
                database.transactionDbProvider.deleteMultisigTransactions(publicAddress)
            }
            walletDbProvider.deleteWalletAddress(addrId)
        }
    }

    suspend fun saveWalletAddressLabel(database: WalletDbProvider, addrId: Long, label: String?) {
        database.updateWalletAddressLabel(addrId, if (label.isNullOrBlank()) null else label)
    }
}