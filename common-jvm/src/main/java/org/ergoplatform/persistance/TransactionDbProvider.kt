package org.ergoplatform.persistance

interface TransactionDbProvider {

    suspend fun insertOrUpdateAddressTransaction(addressTransaction: AddressTransaction)

    /**
     * loads all transactions for a given address, in descending order by inclusion height
     */
    suspend fun loadAddressTransactions(address: String, limit: Int, page: Int): List<AddressTransaction>

    suspend fun insertOrUpdateAddressTransactionToken(addressTxToken: AddressTransactionToken)

    /**
     * loads all tokens for a given transactions and address
     */
    suspend fun loadAddressTransactionTokens(address: String, txId: String): List<AddressTransactionToken>
}