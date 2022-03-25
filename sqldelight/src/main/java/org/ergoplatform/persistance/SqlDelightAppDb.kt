package org.ergoplatform.persistance

class SqlDelightAppDb(val appDatabase: AppDatabase): IAppDatabase {
    override val walletDbProvider: SqlDelightWalletProvider
        get() = SqlDelightWalletProvider(this)
    override val tokenDbProvider: TokenDbProvider
        get() = SqlDelightTokenDbProvider(appDatabase)
    override val transactionDbProvider: TransactionDbProvider
        get() = SqlDelightTransactionDbProvider(appDatabase)
}