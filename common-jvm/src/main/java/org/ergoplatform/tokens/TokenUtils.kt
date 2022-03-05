package org.ergoplatform.tokens

import org.ergoplatform.appkit.Eip4Token
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.WalletToken

/**
 * logic to fill tokens in wallet overview screen
 */
fun fillTokenOverview(
    tokens: List<WalletToken>,
    addToken: (WalletToken) -> Unit,
    addMoreTokenHint: (Int) -> Unit
) {
    val maxTokensToShow = 5
    val dontShowAll = tokens.size > maxTokensToShow
    val tokensSorted = tokens.sortedBy { it.name?.lowercase() }
    val tokensToShow =
        (if (dontShowAll) tokensSorted.subList(
            0,
            maxTokensToShow - 1
        ) else tokensSorted)
    tokensToShow.forEach {
        addToken.invoke(it)
    }

    // in case we don't show all items, add a hint that not all items were shown
    if (dontShowAll) {
        addMoreTokenHint.invoke((tokens.size - maxTokensToShow + 1))
    }
}

/**
 * @return if this is a singular token, or so-called NFT
 */
fun WalletToken.isSingularToken(): Boolean {
    return amount == 1L && decimals == 0
}

/**
 * @return if this is a singular token, or so-called NFT
 */
fun TokenInformation.isSingularToken(): Boolean {
    return fullSupply == 1L && decimals == 0
}

/**
 * @return if this is a singular token, or so-called NFT
 */
fun Eip4Token.isSingularToken(): Boolean {
    return value == 1L && decimals == 0
}

fun Eip4Token.getHttpContentLink(preferencesProvider: PreferencesProvider): String? {
    return nftContentLink?.let { contentLink ->
        return if (contentLink.startsWith("ipfs://"))
            contentLink.replace("ipfs://", preferencesProvider.prefIpfsGatewayUrl + "ipfs/")
        else if (contentLink.startsWith("http://") || contentLink.startsWith("https://"))
            contentLink
        else
            null
    }
}