package org.ergoplatform.persistance

import org.ergoplatform.mosaik.MosaikDbProvider

/**
 * This interface provides getters for all available db provider implementations.
 * Platform specific database implementations should implement this interface to be accessed in
 * platform agnostic code.
 */
interface IAppDatabase {
    val walletDbProvider: WalletDbProvider
    val tokenDbProvider: TokenDbProvider
    val transactionDbProvider: TransactionDbProvider
    val mosaikDbProvider: MosaikDbProvider
    val addressBookDbProvider: AddressBookDbProvider
}