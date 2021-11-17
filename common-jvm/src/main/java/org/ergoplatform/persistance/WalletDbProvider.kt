package org.ergoplatform.persistance

interface WalletDbProvider {
    suspend fun <R> withTransaction(block: suspend () -> R): R

    // Address functions
    suspend fun loadWalletByFirstAddress(firstAddress: String): WalletConfig?

    // Config functions
    suspend fun loadWalletConfigById(id: Int): WalletConfig?
    suspend fun updateWalletConfig(walletConfig: WalletConfig)
    suspend fun insertWalletConfig(walletConfig: WalletConfig)
    fun getAllWalletConfigsSynchronous(): List<WalletConfig>

    // State functions
    suspend fun loadWalletWithStateById(id: Int): Wallet?
    suspend fun insertWalletStates(walletStates: List<WalletState>)

    // Address functions
    suspend fun loadWalletAddresses(firstAddress: String): List<WalletAddress>

    // Token functions
    suspend fun deleteTokensByAddress(publicAddress: String)
    suspend fun insertWalletTokens(walletTokens: List<WalletToken>)
}