package org.ergoplatform.persistance

import org.ergoplatform.getDefaultExplorerApiUrl
import org.ergoplatform.isErgoMainNet

const val NAME_SHAREDPREFS = "ergowallet"
const val KEY_FIAT_CURRENCY = "fiatCurrency"
const val KEY_FIAT_VALUE = "fiatValue"
const val KEY_NODE_URL = "nodeUrl"
const val KEY_NODE_LIST = "nodeList"
const val KEY_EXPLORER_API_URL = "explorerApiUrl"
const val KEY_IPFS_GATEWAY_URL = "ipfsGatewayUrl"
const val KEY_TOKEN_VERIFY_URL = "tokenVerificationUrl"
const val KEY_DOWNLOAD_NFT_CONTENT = "downloadNftContent"
const val KEY_SEND_TX_MESSAGES = "sendTxMessages"
const val KEY_INPUT_FIAT_AMOUNT = "inputFiatAmount"
const val KEY_LASTREFRESH = "lastRefreshMs"
const val KEY_LASTNODELISTREFRESH = "lastNodeListRefresh"
const val KEY_MOSAIK_ENABLED = "enableMosaik"
const val FIAT_CURRENCY_DEFAULT = "usd"

private const val DEFAULT_IPFS_GATEWAY = "https://cloudflare-ipfs.com/"
private const val DEFAULT_TOKEN_VERIFY_URL = "https://api.tokenjay.app/"

abstract class PreferencesProvider {

    abstract fun getString(key: String, default: String): String
    abstract fun saveString(key: String, value: String)
    abstract fun getLong(key: String, default: Long): Long
    abstract fun saveLong(key: String, value: Long)
    abstract fun getFloat(key: String, default: Float): Float
    abstract fun saveFloat(key: String, value: Float)

    protected fun getBoolean(key: String, default: Boolean): Boolean =
        getLong(key, if (default) 1 else 0) != 0L

    protected fun saveBoolean(key: String, value: Boolean) {
        saveLong(key, if (value) 1L else 0L)
    }

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

    var knownNodesList: List<String>
        get() = getString(KEY_NODE_LIST, "").split(',').filterNot { it.isBlank() }
        set(value) {
            saveString(KEY_NODE_LIST, value.joinToString(","))
            saveLong(KEY_LASTNODELISTREFRESH, System.currentTimeMillis())
        }

    fun getDefaultNodeApiUrl() =
        if (isErgoMainNet) "http://159.65.11.55:9053/"
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
        get() = getBoolean(KEY_DOWNLOAD_NFT_CONTENT, false)
        set(value) {
            saveBoolean(KEY_DOWNLOAD_NFT_CONTENT, value)
        }

    var mosaikEnabled: Boolean
        get() = getBoolean(KEY_MOSAIK_ENABLED, false)
        set(value) {
            saveBoolean(KEY_MOSAIK_ENABLED, value)
        }

    var sendTxMessages: Boolean
        get() = getBoolean(KEY_SEND_TX_MESSAGES, false)
        set(value) {
            saveBoolean(KEY_SEND_TX_MESSAGES, value)
        }

    val defaultTokenVerificationUrl = DEFAULT_TOKEN_VERIFY_URL

    var prefTokenVerificationUrl: String
        get() = getString(KEY_TOKEN_VERIFY_URL, DEFAULT_TOKEN_VERIFY_URL)
        set(value) {
            var tokenVerificationUrl = value
            if (tokenVerificationUrl.isEmpty()) {
                tokenVerificationUrl = DEFAULT_TOKEN_VERIFY_URL
            } else if (!tokenVerificationUrl.endsWith("/")) {
                tokenVerificationUrl += "/"
            }

            saveString(KEY_TOKEN_VERIFY_URL, tokenVerificationUrl)
        }

    var lastRefreshMs: Long
        get() = getLong(KEY_LASTREFRESH, 0)
        set(time) {
            saveLong(KEY_LASTREFRESH, time)
        }

    val lastNodeListRefreshMs: Long
        get() = getLong(KEY_LASTNODELISTREFRESH, 0)

    var lastFiatValue: Float
        get() = getFloat(KEY_FIAT_VALUE, 0f)
        set(value) {
            saveFloat(KEY_FIAT_VALUE, value)
        }

    var isSendInputFiatAmount: Boolean
        get() = getBoolean(KEY_INPUT_FIAT_AMOUNT, false)
        set(value) {
            saveBoolean(KEY_INPUT_FIAT_AMOUNT, value)
        }
}