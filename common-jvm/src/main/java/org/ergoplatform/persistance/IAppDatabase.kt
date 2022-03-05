package org.ergoplatform.persistance

interface IAppDatabase {
    val walletDbProvider: WalletDbProvider
    val tokenDbProvider: TokenDbProvider
}