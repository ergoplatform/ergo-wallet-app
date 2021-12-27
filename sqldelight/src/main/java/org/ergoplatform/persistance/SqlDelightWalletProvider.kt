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

    override suspend fun loadWalletConfigById(id: Int): WalletConfig? {
        return withContext(Dispatchers.IO) {
            appDb.walletConfigQueries.loadWalletById(id.toLong()).executeAsOneOrNull()?.toModel()
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

    fun updateWalletDisplayName(displayName: String, walletId: Int) {
        appDb.walletConfigQueries.updateWalletDisplayNameById(displayName, walletId.toLong())
    }

    fun updateWalletDisplayTokens(displayTokens: Boolean, walletId: Int) {
        appDb.walletConfigQueries.updateWalletTokensUnfold(displayTokens, walletId.toLong())
    }

    override suspend fun insertWalletConfig(walletConfig: WalletConfig) {
        // same code here
        updateWalletConfig(walletConfig)
    }

    suspend fun deleteAllWalletData(walletConfig: WalletConfig) {
        withTransaction {
            walletConfig.firstAddress?.let { firstAddress ->
                appDb.walletStateQueries.deleteByFirstAddress(firstAddress)
                appDb.walletTokenQueries.deleteTokensByFirstAddress(firstAddress)
                appDb.walletAddressQueries.deleteWalletAddressByFirstAddress(firstAddress)
            }
            appDb.walletConfigQueries.deleteWalletById(walletConfig.id.toLong())
        }
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
        fetchStateInformation(model)
    }

    private fun fetchStateInformation(model: WalletConfig): Wallet {
        val state = appDb.walletStateQueries.loadWalletStates(model.firstAddress!!)
            .executeAsList().map { it.toModel() }
        val tokens = appDb.walletTokenQueries.loadWalletTokens(model.firstAddress!!)
            .executeAsList().map { it.toModel() }
        val addresses =
            appDb.walletAddressQueries.loadWalletAddresses(model.firstAddress!!)
                .executeAsList().map { it.toModel() }

        return Wallet(model, state, tokens, addresses)
    }

    override suspend fun loadWalletWithStateById(id: Int): Wallet? {
        return withContext(Dispatchers.IO) {
            val walletConfig =
                appDb.walletConfigQueries.loadWalletById(id.toLong()).executeAsOneOrNull()
                    ?.toModel()
            walletConfig?.let { fetchStateInformation(it) }
        }
    }

    override suspend fun walletWithStateByIdAsFlow(id: Int): Flow<Wallet?> {
        return flow {
            appDb.walletConfigQueries.observeWithState().asFlow().collect {
                // we detected a change in any of the database tables - do all queries and return
                emit(loadWalletWithStateById(id))
            }
        }
    }

    override suspend fun loadWalletAddresses(firstAddress: String): List<WalletAddress> {
        return withContext(Dispatchers.IO) {
            appDb.walletAddressQueries.loadWalletAddresses(firstAddress).executeAsList()
                .map { it.toModel() }
        }
    }

    override suspend fun insertWalletAddress(walletAddress: WalletAddress) {
        withContext(Dispatchers.IO) {
            appDb.walletAddressQueries.insertOrReplace(
                if (walletAddress.id > 0) walletAddress.id else null,
                walletAddress.walletFirstAddress,
                walletAddress.derivationIndex,
                walletAddress.publicAddress,
                walletAddress.label
            )
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