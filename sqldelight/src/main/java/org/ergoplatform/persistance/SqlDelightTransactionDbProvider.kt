package org.ergoplatform.persistance

class SqlDelightTransactionDbProvider(private val sqlDelightAppDb: SqlDelightAppDb) :
    TransactionDbProvider() {
    private val appDatabase = sqlDelightAppDb.appDatabase

    override suspend fun insertOrUpdateAddressTransaction(addressTransaction: AddressTransaction) {
        sqlDelightAppDb.useIoContext {
            val tx = addressTransaction.toDbEntity()
            appDatabase.addressTransactionQueries.insertOrReplace(
                if (tx.id > 0) tx.id else null,
                tx.address,
                tx.tx_id,
                tx.inclusion_height,
                tx.timestamp,
                tx.nanoerg,
                tx.message,
                tx.state
            )
        }
    }

    override suspend fun loadAddressTransactions(
        address: String,
        limit: Int,
        page: Int
    ): List<AddressTransaction> {
        return sqlDelightAppDb.useIoContext {
            appDatabase.addressTransactionQueries.loadAddressTransactions(
                address,
                limit.toLong(),
                page * limit.toLong()
            ).executeAsList().map { it.toModel() }
        }
    }

    override suspend fun deleteAddressTransactions(address: String) {
        sqlDelightAppDb.useIoContext {
            appDatabase.addressTransactionQueries.deleteByAddress(address)
            appDatabase.addressTransactionTokenQueries.deleteByAddress(address)
        }
    }

    override suspend fun deleteTransaction(id: Int) {
        sqlDelightAppDb.useIoContext {
            appDatabase.addressTransactionQueries.loadAddressTransaction(id.toLong())
                .executeAsOneOrNull()?.let { addressTransaction ->
                    appDatabase.addressTransactionTokenQueries.deleteAddressTxTokens(
                        addressTransaction.address,
                        addressTransaction.tx_id
                    )
                    appDatabase.addressTransactionQueries.deleteById(addressTransaction.id)
                }
        }
    }

    override suspend fun insertOrUpdateAddressTransactionToken(addressTxToken: AddressTransactionToken) {
        sqlDelightAppDb.useIoContext {
            val txToken = addressTxToken.toDbEntity()
            appDatabase.addressTransactionTokenQueries.insertOrReplace(
                if (txToken.id > 0) txToken.id else null,
                txToken.address,
                txToken.tx_id,
                txToken.token_id,
                txToken.name,
                txToken.amount,
                txToken.decimals
            )
        }
    }

    override suspend fun loadAddressTransactionTokens(
        address: String,
        txId: String
    ): List<AddressTransactionToken> {
        return sqlDelightAppDb.useIoContext {
            appDatabase.addressTransactionTokenQueries.loadAddressTxTokens(address, txId)
                .executeAsList().map { it.toModel() }
        }
    }

}
