package org.ergoplatform.persistance

import org.ergoplatform.getDefaultExplorerApiUrl
import org.ergoplatform.isErgoMainNet

const val NAME_SHAREDPREFS = "ergowallet"
const val KEY_FIAT_CURRENCY = "fiatCurrency"
const val KEY_FIAT_VALUE = "fiatValue"
const val KEY_NODE_URL = "nodeUrl"
const val KEY_EXPLORER_API_URL = "explorerApiUrl"
const val KEY_IPFS_GATEWAY_URL = "ipfsGatewayUrl"
const val KEY_DOWNLOAD_NFT_CONTENT = "downloadNftContent"
const val KEY_LASTREFRESH = "lastRefreshMs"
const val FIAT_CURRENCY_DEFAULT = "usd"

private const val DEFAULT_IPFS_GATEWAY = "https://cloudflare-ipfs.com/"

abstract class PreferencesProvider {

    abstract fun getString(key: String, default: String): String
    abstract fun saveString(key: String, value: String)
    abstract fun getLong(key: String, default: Long): Long
    abstract fun saveLong(key: String, value: Long)
    abstract fun getFloat(key: String, default: Float): Float
    abstract fun saveFloat(key: String, value: Float)

    var prefDisplayCurrency: String
        get() {
            return getString(KEY_FIAT_CURRENCY, FIAT_CURRENCY_DEFAULT)
        }
        set(currency) {
            saveString(KEY_FIAT_CURRENCY, currency)
            lastFiatValue = 0.0f
        }

    var prefNodeUrl: String
        get() = getString(KEY_NODE_URL, getDefaultNodeApiUrl())
        set(nodeUrl) {
            var savedNodeUrl = nodeUrl
            if (savedNodeUrl.isEmpty()) {
                savedNodeUrl = getDefaultNodeApiUrl()
            } else if (!savedNodeUrl.endsWith("/")) {
                savedNodeUrl += "/"
            }

            saveString(KEY_NODE_URL, savedNodeUrl)
        }

    fun getDefaultNodeApiUrl() =
        if (isErgoMainNet) "http://213.239.193.208:9053/"
        else "http://213.239.193.208:9052/"

    var prefExplorerApiUrl: String
        get() = getString(KEY_EXPLORER_API_URL, getDefaultExplorerApiUrl())
        set(value) {
            var savedExplorerApiUrl = value
            if (savedExplorerApiUrl.isEmpty()) {
                savedExplorerApiUrl = getDefaultExplorerApiUrl()
            } else if (!savedExplorerApiUrl.endsWith("/")) {
                savedExplorerApiUrl += "/"
            }

            saveString(KEY_EXPLORER_API_URL, savedExplorerApiUrl)
        }

    val defaultIpfsGatewayUrl = DEFAULT_IPFS_GATEWAY

    var prefIpfsGatewayUrl: String
        // alts: https://gateway.pinata.cloud https://ipfs.best-practice.se
        get() = getString(KEY_IPFS_GATEWAY_URL, DEFAULT_IPFS_GATEWAY)
        set(value) {
            var ipfsGatewayUrl = value
            if (ipfsGatewayUrl.isEmpty()) {
                ipfsGatewayUrl = DEFAULT_IPFS_GATEWAY
            } else if (!ipfsGatewayUrl.endsWith("/")) {
                ipfsGatewayUrl += "/"
            }

            saveString(KEY_IPFS_GATEWAY_URL, ipfsGatewayUrl)
        }

    var downloadNftContent: Boolean
        get() = getLong(KEY_DOWNLOAD_NFT_CONTENT, 0) != 0L
        set(value) {
            saveLong(KEY_DOWNLOAD_NFT_CONTENT, if (value) 1L else 0L)
        }

    var lastRefreshMs: Long
        get() = getLong(KEY_LASTREFRESH, 0)
        set(time) {
            saveLong(KEY_LASTREFRESH, time)
        }

    var lastFiatValue: Float
        get() = getFloat(KEY_FIAT_VALUE, 0f)
        set(value) {
            saveFloat(KEY_FIAT_VALUE, value)
        }
}