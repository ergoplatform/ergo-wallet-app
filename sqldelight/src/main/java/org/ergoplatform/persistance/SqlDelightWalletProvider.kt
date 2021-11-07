package org.ergoplatform.persistance

import com.squareup.sqldelight.runtime.coroutines.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
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
                if (walletConfig.id > 0) walletConfig.id.toLong() else null,
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

    fun getWalletsWithStatesFlow(): Flow<List<Wallet>> {
        return flow {
            appDb.walletConfigQueries.observeWithState().asFlow().collect {
                // we detected a change in any of the database tables - do all queries and return
                emit(getWalletsWithStates())
            }
        }
    }

    fun getWalletsWithStates() = appDb.walletConfigQueries.selectAll().executeAsList().map {
        val model = it.toModel()
        val state = appDb.walletStateQueries.loadWalletStates(model.firstAddress!!)
            .executeAsList().map { it.toModel() }
        val tokens = appDb.walletTokenQueries.loadWalletTokens(model.firstAddress!!)
            .executeAsList().map { it.toModel() }
        val addresses =
            appDb.walletAddressQueries.loadWalletAddresses(model.firstAddress!!)
                .executeAsList().map { it.toModel() }

        Wallet(model, state, tokens, addresses)
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
                    if (it.id > 0) it.id else null,
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