package org.ergoplatform.persistance

class SqlDelightAppDb(private val appDatabase: AppDatabase): IAppDatabase {
    override val walletDbProvider: SqlDelightWalletProvider
        get() = SqlDelightWalletProvider(appDatabase)
    override val tokenDbProvider: TokenDbProvider
        get() = SqlDelightTokenDbProvider(appDatabase)
}