package org.ergoplatform.tokens

import org.ergoplatform.WalletStateSyncManager

object TokenVerifier {
    private val genuineTokens: List<Eip21Token> = listOf(
        Eip21Token(
            "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
            "SigUSD",
            true,
            "sigmausd.io"
        ),
        Eip21Token(
            "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
            "SigRSV",
            true,
            "sigmausd.io"
        ),
    )

    fun checkTokenGenuine(tokenId: String, tokenDisplayName: String): Eip21Token? {
        // check EIP-21
        val eip21token = genuineTokens.find { it.tokenId.equals(tokenId, true) }
            ?: genuineTokens.find { it.uniqueName && it.displayName.equals(tokenDisplayName, true) }

        if (eip21token != null)
            return eip21token

        // if this token is not found for EIP-21, check the price list
        val priceTokenList = WalletStateSyncManager.getInstance().tokenPrices
        return (priceTokenList[tokenId] ?: priceTokenList.values.find {
            it.displayName.equals(
                tokenDisplayName,
                true
            )
        })?.let {
            Eip21Token(it.tokenId, it.displayName ?: "???", true, null)
        }
    }
}

data class Eip21Token(
    val tokenId: String,
    val displayName: String,
    val uniqueName: Boolean,
    val issuer: String?
)