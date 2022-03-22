package org.ergoplatform.persistance

interface TransactionDbProvider {

    suspend fun insertOrUpdateAddressTransaction(addressTransaction: AddressTransaction)

    /**
     * loads all transactions for a given address, in descending order by inclusion height
     */
    suspend fun loadAddressTransactions(address: String, limit: Int, page: Int): List<AddressTransaction>

    /**
     * deletes all address transactions for a given address - should be called from within a db transaction
     */
    suspend fun deleteAddressTransactions(address: String)

    /**
     * deletes transaction and dependant tokens
     */
    suspend fun deleteTransaction(id: Int)

    suspend fun insertOrUpdateAddressTransactionToken(addressTxToken: AddressTransactionToken)
    /**
     * loads all tokens for a given transactions and address
     */
    suspend fun loadAddressTransactionTokens(address: String, txId: String): List<AddressTransactionToken>
}