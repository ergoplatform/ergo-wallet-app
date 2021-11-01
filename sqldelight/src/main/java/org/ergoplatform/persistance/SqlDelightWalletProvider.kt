package org.ergoplatform.persistance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SqlDelightWalletProvider(private val appDb: AppDatabase) : WalletDbProvider {
    override suspend fun <R> withTransaction(block: suspend () -> R): R {
        return withContext(Dispatchers.IO) {
            appDb.transactionWithResult {
                runBlocking {
                    block.invoke()
                }
            }
        }
    }

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

    override fun getAllWalletConfigsSynchronous(): List<WalletConfig> {
        return appDb.walletConfigQueries.selectAll().executeAsList().map { it.toModel() }
    }


    override suspend fun insertWalletStates(walletStates: List<WalletState>) {
        withContext(Dispatchers.IO) {
            walletStates.forEach {
                appDb.walletStateQueries.insertOrReplace(it.toDbEntity())
            }
        }
    }


    override suspend fun loadWalletAddresses(firstAddress: String): List<WalletAddress> {
        return withContext(Dispatchers.IO) {
            appDb.walletAddressQueries.loadWalletAddresses(firstAddress).executeAsList()
                .map { it.toModel() }
        }
    }

    override suspend fun deleteTokensByAddress(publicAddress: String) {
        withContext(Dispatchers.IO) {
            appDb.walletTokenQueries.deleteTokensByAddress(publicAddress)
        }
    }

    override suspend fun insertWalletTokens(walletTokens: List<WalletToken>) {
        withContext(Dispatchers.IO) {
            walletTokens.forEach {
                appDb.walletTokenQueries.insertOrReplace(
                    it.id,
                    it.publicAddress,
                    it.walletFirstAddress,
                    it.tokenId,
                    it.amount,
                    it.decimals,
                    it.name
                )
            }
        }
    }
}