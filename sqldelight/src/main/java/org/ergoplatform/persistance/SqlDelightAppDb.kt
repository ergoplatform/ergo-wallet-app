package org.ergoplatform.persistance

class SqlDelightAppDb(
    val appDatabase: AppDatabase,
    /**
     * if set to false, no transactions will be used for SQlite operations.
     * Use on iOS as its SQlite version cannot handle transactions
     */
    val useTransactions: Boolean = true
) : IAppDatabase {

    override val walletDbProvider: SqlDelightWalletProvider
        get() = SqlDelightWalletProvider(this)
    override val tokenDbProvider: TokenDbProvider
        get() = SqlDelightTokenDbProvider(appDatabase)
    override val transactionDbProvider: TransactionDbProvider
        get() = SqlDelightTransactionDbProvider(appDatabase)

}