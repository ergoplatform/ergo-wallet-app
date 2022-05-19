package org.ergoplatform.api

import org.ergoplatform.persistance.TokenPrice

interface TokenPriceApi {
    fun getTokenPrices(): List<Pair<TokenPrice, PriceImportance>>?
}

/**
 * Price importance determines which price is saved and shown
 */
enum class PriceImportance {
    /**
     * price is not of practical use, use it only as a fallback
     */
    VeryLow,

    /**
     * price on non-mobile or in other ways restricted sites
     */
    Low,

    Normal,

    /**
     * for official issuers, index price, ...
     */
    High
}