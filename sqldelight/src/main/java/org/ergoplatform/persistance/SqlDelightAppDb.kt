package org.ergoplatform.persistance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ergoplatform.mosaik.MosaikDbProvider

class SqlDelightAppDb(
    val appDatabase: AppDatabase,
) : IAppDatabase {

    var inTransaction = false

    /**
     * switches to IO coroutine context if no transaction is open at the moment
     * if we are already in a transaction, do not switch the coroutines context this will cause
     * an Exception on desktop or freeze on iOS due to db being locked to a single thread
     */
    suspend fun <R> useIoContext(block: suspend  () -> R): R {
        return if (!inTransaction)
            withContext(Dispatchers.IO) {
                block.invoke()
            }
        else
            block.invoke()
    }

    override val walletDbProvider: SqlDelightWalletProvider
        get() = SqlDelightWalletProvider(this)
    override val tokenDbProvider: TokenDbProvider
        get() = SqlDelightTokenDbProvider(this)
    override val transactionDbProvider: TransactionDbProvider
        get() = SqlDelightTransactionDbProvider(this)
    override val mosaikDbProvider: MosaikDbProvider
        get() = SqlDelightMosaikDbProvider(this)
    override val addressBookDbProvider: AddressBookDbProvider
        get() = SqlDelightAddressBookDbProvider(this)
}