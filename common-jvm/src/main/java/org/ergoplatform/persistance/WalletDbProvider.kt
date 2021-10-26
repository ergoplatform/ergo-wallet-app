package org.ergoplatform.persistance

interface WalletDbProvider {
    suspend fun loadWalletByFirstAddress(firstAddress: String): WalletConfig?
    suspend fun updateWalletConfig(walletConfig: WalletConfig)
    suspend fun insertWalletConfig(walletConfig: WalletConfig)
}