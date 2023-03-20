package org.ergoplatform.persistance

import com.squareup.sqldelight.runtime.coroutines.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SqlDelightWalletProvider(private val sqlDelightAppDb: SqlDelightAppDb) : WalletDbProvider {
    private val appDb = sqlDelightAppDb.appDatabase

    override suspend fun <R> withTransaction(block: suspend () -> R): R {
        // if we already are in a transaction, do not open a subtransaction
        return if (!sqlDelightAppDb.inTransaction)
            withContext(Dispatchers.IO) {
                sqlDelightAppDb.inTransaction = true
                val result: R = appDb.transactionWithResult {
                    runBlocking {
                        block.invoke()
                    }
                }
                sqlDelightAppDb.inTransaction = false
                result
            }
        else
            block.invoke()
    }

    override suspend fun loadWalletByFirstAddress(firstAddress: String): WalletConfig? {
        return sqlDelightAppDb.useIoContext {
            appDb.walletConfigQueries.loadWalletByFirstAddress(firstAddress)
                .executeAsOneOrNull()?.toModel()
        }
    }

    override suspend fun loadWalletConfigById(id: Int): WalletConfig? {
        return sqlDelightAppDb.useIoContext {
            appDb.walletConfigQueries.loadWalletById(id.toLong()).executeAsOneOrNull()?.toModel()
        }
    }

    override suspend fun updateWalletConfig(walletConfig: WalletConfig) {
        sqlDelightAppDb.useIoContext {
            appDb.walletConfigQueries.insertOrReplace(
                if (walletConfig.id > 0) walletConfig.id.toLong() else null,
                walletConfig.displayName,
                walletConfig.firstAddress,
                walletConfig.encryptionType,
                walletConfig.secretStorage,
                walletConfig.unfoldTokens,
                walletConfig.extendedPublicKey,
                walletConfig.walletType,
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

    override suspend fun deleteWalletConfigAndStates(firstAddress: String, walletId: Int?) {
        loadWalletAddresses(firstAddress).forEach {
            sqlDelightAppDb.transactionDbProvider.deleteAddressTransactions(it.publicAddress)
        }

        appDb.walletStateQueries.deleteByFirstAddress(firstAddress)
        appDb.walletTokenQueries.deleteTokensByFirstAddress(firstAddress)
        appDb.walletAddressQueries.deleteWalletAddressByFirstAddress(firstAddress)
        sqlDelightAppDb.transactionDbProvider.deleteAddressTransactions(firstAddress)
        appDb.multisigTransactionQueries.deleteByAddress(firstAddress)
        (walletId ?: loadWalletByFirstAddress(firstAddress)?.id)?.let { id ->
            appDb.walletConfigQueries.deleteWalletById(id.toLong())
        }
    }

    suspend fun deleteAllWalletData(walletConfig: WalletConfig) {
        withTransaction {
            walletConfig.firstAddress?.let { firstAddress ->
                deleteWalletConfigAndStates(firstAddress, walletConfig.id)
            } ?: appDb.walletConfigQueries.deleteWalletById(walletConfig.id.toLong())
        }
    }

    override fun getAllWalletConfigsSynchronous(): List<WalletConfig> {
        return appDb.walletConfigQueries.selectAll().executeAsList().map { it.toModel() }
    }


    override suspend fun insertWalletStates(walletStates: List<WalletState>) {
        sqlDelightAppDb.useIoContext {
            walletStates.forEach {
                appDb.walletStateQueries.insertOrReplace(it.toDbEntity())
            }
        }
    }

    override suspend fun deleteAddressState(publicAddress: String) {
        sqlDelightAppDb.useIoContext {
            appDb.walletStateQueries.deleteAddressState(publicAddress)
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
        return sqlDelightAppDb.useIoContext {
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
        return sqlDelightAppDb.useIoContext {
            appDb.walletAddressQueries.loadWalletAddresses(firstAddress).executeAsList()
                .map { it.toModel() }
        }
    }

    override suspend fun loadWalletAddress(id: Long): WalletAddress? {
        return sqlDelightAppDb.useIoContext {
            appDb.walletAddressQueries.loadWalletAddress(id).executeAsOneOrNull()?.toModel()
        }
    }

    override suspend fun loadWalletAddress(publicAddress: String): WalletAddress? {
        return sqlDelightAppDb.useIoContext {
            appDb.walletAddressQueries.loadWalletAddressByPk(publicAddress).executeAsOneOrNull()?.toModel()
        }
    }

    override suspend fun insertWalletAddress(walletAddress: WalletAddress) {
        // do not use withContext(sqlDelightAppDb.dispatcher) here. Caused freeze on iOS and the only caller
        // WalletAddressesUiLogic calls in IO context anyway.
            appDb.walletAddressQueries.insertOrReplace(
                if (walletAddress.id > 0) walletAddress.id else null,
                walletAddress.walletFirstAddress,
                walletAddress.derivationIndex,
                walletAddress.publicAddress,
                walletAddress.label
            )
    }

    override suspend fun updateWalletAddressLabel(addrId: Long, newLabel: String?) {
        sqlDelightAppDb.useIoContext {
            appDb.walletAddressQueries.updateLabel(newLabel, addrId)
        }
    }

    override suspend fun deleteWalletAddress(addrId: Long) {
        sqlDelightAppDb.useIoContext {
            appDb.walletAddressQueries.deleteWalletAddress(addrId)
        }
    }

    override suspend fun deleteTokensByAddress(publicAddress: String) {
        sqlDelightAppDb.useIoContext {
            appDb.walletTokenQueries.deleteTokensByAddress(publicAddress)
        }
    }

    override suspend fun insertWalletTokens(walletTokens: List<WalletToken>) {
        sqlDelightAppDb.useIoContext {
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