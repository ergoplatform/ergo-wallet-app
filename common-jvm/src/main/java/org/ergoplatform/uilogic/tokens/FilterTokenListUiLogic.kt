package org.ergoplatform.uilogic.tokens

import org.ergoplatform.persistance.TokenInformation

/**
 * Helper methods for filterable token lists
 */
interface FilterTokenListUiLogic {
    val tokenFilterMap: MutableMap<Int, Boolean>

    fun toggleTokenFilter(filterType: Int) {
        tokenFilterMap[filterType] = !(tokenFilterMap[filterType] ?: false)
        onFilterChanged()
    }

    fun hasTokenFilter(filterType: Int): Boolean =
        tokenFilterMap[filterType] ?: false

    fun isTokenInFilter(ti: TokenInformation?): Boolean {
        println(tokenFilterMap.values)
        return tokenFilterMap.values.none { it }
                || ti?.thumbnailType?.let { tokenFilterMap[it] == true } ?: false
    }

    fun onFilterChanged() {}
}