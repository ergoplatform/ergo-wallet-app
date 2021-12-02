package org.ergoplatform.tokens

import org.ergoplatform.persistance.WalletToken

/**
 * logic to fill tokens in wallet overview screen
 */
fun fillTokenOverview(tokens: List<WalletToken>, addToken: (WalletToken) -> Unit, addMoreTokenHint: (Int) -> Unit) {
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