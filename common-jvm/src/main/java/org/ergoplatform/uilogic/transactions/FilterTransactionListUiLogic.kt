package org.ergoplatform.uilogic.transactions

/**
 * Helper interface for filtering transaction lists
 */
interface FilterTransactionListUiLogic {
    /**
     * Currently selected token filter. null = show all transactions (ERG and all tokens)
     * empty string = show only ERG transactions
     * token ID = show only transactions involving this specific token
     */
    var selectedTokenFilter: String?

    /**
     * Sets the token filter and refreshes the display
     * @param tokenId null for all, empty string for ERG only, or token ID for specific token
     */
    fun setTokenFilter(tokenId: String?) {
        selectedTokenFilter = tokenId
        onFilterChanged()
    }

    /**
     * Returns true if a transaction should be shown based on current filter
     */
    fun isTransactionInFilter(transaction: AddressTransactionWithTokens): Boolean {
        return when (selectedTokenFilter) {
            null -> true  // Show all transactions
            "" -> transaction.tokens.isEmpty()  // Show only ERG-only transactions
            else -> transaction.tokens.any { it.tokenId == selectedTokenFilter }  // Show transactions with specific token
        }
    }

    /**
     * Gets the list of all unique tokens from a list of transactions
     * Useful for building filter dropdown
     */
    fun getUniqueTokensFromTransactions(transactions: List<AddressTransactionWithTokens>): List<Pair<String, String>> {
        val tokenMap = mutableMapOf<String, String>()
        transactions.forEach { tx ->
            tx.tokens.forEach { token ->
                if (!tokenMap.containsKey(token.tokenId)) {
                    tokenMap[token.tokenId] = token.name
                }
            }
        }
        return tokenMap.map { Pair(it.key, it.value) }.sortedBy { it.second.lowercase() }
    }

    /**
     * Filters a transaction list based on the current filter setting
     */
    fun filterTransactionList(transactions: List<AddressTransactionWithTokens>): List<AddressTransactionWithTokens> {
        return if (selectedTokenFilter == null) {
            transactions
        } else {
            transactions.filter { isTransactionInFilter(it) }
        }
    }

    /**
     * Called when filter changes - implement this to refresh UI
     */
    fun onFilterChanged() {}
}
