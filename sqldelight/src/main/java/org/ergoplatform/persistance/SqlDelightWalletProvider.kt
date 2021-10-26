package org.ergoplatform.persistance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightWalletProvider(private val appDb: AppDatabase) : WalletDbProvider {
    override suspend fun loadWalletByFirstAddress(firstAddress: String): WalletConfig? {
        return withContext(Dispatchers.IO) {
            appDb.walletConfigQueries.loadWalletByFirstAddress(firstAddress)
                .executeAsOneOrNull()?.toModel()
        }
    }

    override suspend fun updateWalletConfig(walletConfig: WalletConfig) {
        withContext(Dispatchers.IO) {
            appDb.walletConfigQueries.insertOrReplace(
                walletConfig.id,
                walletConfig.displayName,
                walletConfig.firstAddress,
                walletConfig.encryptionType,
                walletConfig.secretStorage,
                walletConfig.unfoldTokens
            )
        }
    }

    override suspend fun insertWalletConfig(walletConfig: WalletConfig) {
        // same code here
        updateWalletConfig(walletConfig)
    }
}