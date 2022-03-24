package org.ergoplatform.persistance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqlDelightTransactionDbProvider(private val appDatabase: AppDatabase) :
    TransactionDbProvider() {

    override suspend fun insertOrUpdateAddressTransaction(addressTransaction: AddressTransaction) {
        withContext(Dispatchers.IO) {
            appDatabase.addressTransactionQueries.insertOrReplace(addressTransaction.toDbEntity())
        }
    }

    override suspend fun loadAddressTransactions(
        address: String,
        limit: Int,
        page: Int
    ): List<AddressTransaction> {
        return withContext(Dispatchers.IO) {
            appDatabase.addressTransactionQueries.loadAddressTransactions(
                address,
                limit.toLong(),
                page * limit.toLong()
            ).executeAsList().map { it.toModel() }
        }
    }

    override suspend fun deleteAddressTransactions(address: String) {
        withContext(Dispatchers.IO) {
            appDatabase.addressTransactionQueries.deleteByAddress(address)
            appDatabase.addressTransactionTokenQueries.deleteByAddress(address)
        }
    }

    override suspend fun deleteTransaction(id: Int) {
        withContext(Dispatchers.IO) {
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
        withContext(Dispatchers.IO) {
            appDatabase.addressTransactionTokenQueries.insertOrReplace(addressTxToken.toDbEntity())
        }
    }

    override suspend fun loadAddressTransactionTokens(
        address: String,
        txId: String
    ): List<AddressTransactionToken> {
        return withContext(Dispatchers.IO) {
            appDatabase.addressTransactionTokenQueries.loadAddressTxTokens(address, txId)
                .executeAsList().map { it.toModel() }
        }
    }

}
