package org.ergoplatform.persistance

interface PreferencesProvider {
    var prefDisplayCurrency: String
    var prefNodeUrl: String
    var prefExplorerApiUrl: String
    var dayNightMode: Int
    var lastRefreshMs: Long
    var lastFiatValue: Float
}