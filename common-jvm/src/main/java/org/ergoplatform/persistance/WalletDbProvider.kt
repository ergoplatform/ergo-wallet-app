package org.ergoplatform.persistance

import kotlinx.coroutines.flow.Flow

interface WalletDbProvider {
    suspend fun <R> withTransaction(block: suspend () -> R): R

    // Address functions
    suspend fun loadWalletByFirstAddress(firstAddress: String): WalletConfig?

    // Config functions
    suspend fun loadWalletConfigById(id: Int): WalletConfig?
    suspend fun updateWalletConfig(walletConfig: WalletConfig)
    suspend fun insertWalletConfig(walletConfig: WalletConfig)
    suspend fun deleteWalletConfigAndStates(firstAddress: String, walletId: Int? = null)
    fun getAllWalletConfigsSynchronous(): List<WalletConfig>

    // State functions
    suspend fun loadWalletWithStateById(id: Int): Wallet?
    suspend fun walletWithStateByIdAsFlow(id: Int): Flow<Wallet?>
    suspend fun insertWalletStates(walletStates: List<WalletState>)
    suspend fun deleteAddressState(publicAddress: String)

    // Address functions
    suspend fun loadWalletAddresses(firstAddress: String): List<WalletAddress>
    suspend fun loadWalletAddress(id: Long): WalletAddress?
    suspend fun loadWalletAddress(publicAddress: String): WalletAddress?
    suspend fun insertWalletAddress(walletAddress: WalletAddress)
    suspend fun updateWalletAddressLabel(addrId: Long, newLabel: String?)
    suspend fun deleteWalletAddress(addrId: Long)

    // Token functions
    suspend fun deleteTokensByAddress(publicAddress: String)
    suspend fun insertWalletTokens(walletTokens: List<WalletToken>)
}