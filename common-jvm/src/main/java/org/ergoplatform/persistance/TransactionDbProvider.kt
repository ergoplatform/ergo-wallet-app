package org.ergoplatform.persistance

import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens

abstract class TransactionDbProvider {

    abstract suspend fun insertOrUpdateAddressTransaction(addressTransaction: AddressTransaction)

    abstract suspend fun loadAddressTransaction(
        address: String, txId: String
    ): AddressTransaction?

    /**
     * loads all transactions for a given address, in descending order by inclusion height
     */
    abstract suspend fun loadAddressTransactions(
        address: String,
        limit: Int,
        page: Int
    ): List<AddressTransaction>

    /**
     * deletes all address transactions and tokens for a given address
     * - should be called from within a db transaction
     */
    abstract suspend fun deleteAddressTransactions(address: String)

    /**
     * deletes transaction and dependant tokens
     */
    abstract suspend fun deleteTransaction(id: Int)

    abstract suspend fun insertOrUpdateAddressTransactionToken(addressTxToken: AddressTransactionToken)

    /**
     * loads all tokens for a given transactions and address
     */
    abstract suspend fun loadAddressTransactionTokens(
        address: String,
        txId: String
    ): List<AddressTransactionToken>


    /**
     * same as [loadAddressTransactions], but loading tokens as well
     */
    suspend fun loadAddressTransactionsWithTokens(
        address: String,
        limit: Int,
        page: Int
    ): List<AddressTransactionWithTokens> {
        val transactions = loadAddressTransactions(address, limit, page)
        return transactions.map { tx ->
            AddressTransactionWithTokens(
                tx,
                loadAddressTransactionTokens(tx.address, tx.txId).sortedBy { it.name.lowercase() }
            )
        }
    }

    abstract suspend fun loadMultisigTransactions(address: String): List<MultisigTransaction>

    abstract suspend fun deleteMultisigTransactions(address: String)

    abstract suspend fun insertOrUpdateMultisigTransaction(multisigTransaction: MultisigTransaction)
}