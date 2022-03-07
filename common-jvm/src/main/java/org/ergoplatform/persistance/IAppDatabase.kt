package org.ergoplatform.persistance

/**
 * This interface provides getters for all available db provider implementations.
 * Platform specific database implementations should implement this interface to be accessed in
 * platform agnostic code.
 */
interface IAppDatabase {
    val walletDbProvider: WalletDbProvider
    val tokenDbProvider: TokenDbProvider
}