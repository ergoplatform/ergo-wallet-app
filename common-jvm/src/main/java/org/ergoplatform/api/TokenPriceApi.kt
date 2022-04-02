package org.ergoplatform.api

import org.ergoplatform.persistance.TokenPrice

interface TokenPriceApi {
    fun getTokenPrices(): List<TokenPrice>?
}