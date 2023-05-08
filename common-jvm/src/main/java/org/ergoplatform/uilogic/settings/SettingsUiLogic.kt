package org.ergoplatform.uilogic.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.ErgoFacade
import org.ergoplatform.api.tokenjay.TokenJayApiClient
import org.ergoplatform.isErgoMainNet
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.restapi.client.InfoApi
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import java.util.*
import kotlin.coroutines.coroutineContext

class SettingsUiLogic {
    private val maxNumToConnectTo = 10

    fun getFiatCurrencyButtonText(preferences: PreferencesProvider, texts: StringProvider): String {
        val displayCurrency =
            preferences.prefDisplayCurrency.uppercase(Locale.getDefault())
        return texts.getString(
            STRING_BUTTON_DISPLAY_CURRENCY,
            if (displayCurrency.isNotEmpty()) displayCurrency else texts.getString(STRING_LABEL_NONE)
        )
    }

    fun changeBalanceSyncInterval(prefs: PreferencesProvider) {
        prefs.balanceSyncInterval = when (prefs.balanceSyncInterval) {
            0L -> 1L
            1L -> 2
            2L -> 4
            4L -> 6
            6L -> 12
            12L -> 24
            else -> 0
        }
    }

    fun getBalanceSyncButtonText(prefs: PreferencesProvider, texts: StringProvider): String =
        when (val setting = prefs.balanceSyncInterval) {
            0L -> texts.getString(STRING_BUTTON_BALANCE_NOTIF_NEVER)
            1L -> texts.getString(STRING_BUTTON_BALANCE_NOTIF_HOURLY)
            else -> texts.getString(STRING_BUTTON_BALANCE_NOTIF_XHOURLY, setting.toString())
        }

    val checkNodesState: StateFlow<CheckNodesState> get() = _checkNodesState
    private val _checkNodesState = MutableStateFlow<CheckNodesState>(CheckNodesState.Waiting)
    var lastNodeList: List<NodeInfo> = emptyList()
        private set

    suspend fun checkAvailableNodes(prefs: PreferencesProvider) {
        if (_checkNodesState.value != CheckNodesState.Waiting) return

        // try to connect to current node to fetch a new list
        _checkNodesState.value = CheckNodesState.FetchingNodes
        ErgoFacade.refreshNodeList(prefs)

        var knownNodesList = prefs.knownNodesList

        knownNodesList = knownNodesList.map { it.trimEnd('/') }

        if (knownNodesList.size < maxNumToConnectTo && coroutineContext.isActive && isErgoMainNet) {
            try {
                val peers = TokenJayApiClient().getDetectedNodePeers()
                val highestBlockHeight = peers.firstOrNull()?.blockHeight ?: 0

                knownNodesList = knownNodesList + peers
                    // filter the nodes that are not synced
                    .filter { it.blockHeight >= highestBlockHeight - 30 }
                    .map { it.url.trimEnd('/') }
                    // if we already know it, don't add it again
                    .filter { !knownNodesList.contains(it) }
                    // prefer https nodes
                    .sortedBy { if (it.startsWith("https://")) 0 else 1 }

            } catch (t: Throwable) {
                LogUtils.logDebug("TokenJay API", "Error fetching TokenJay peers", t)
            }
        }

        knownNodesList = knownNodesList.take(maxNumToConnectTo).map { it.trimEnd('/') }

        knownNodesList =
            if (knownNodesList.contains(prefs.prefNodeUrl.trimEnd('/')))
                knownNodesList
            else (listOf(prefs.prefNodeUrl) + knownNodesList).take(maxNumToConnectTo)

        val nodeInfo = knownNodesList.map { nodeUrl ->
            if (coroutineContext.isActive) {
                _checkNodesState.value = CheckNodesState.TestingNode(nodeUrl)
                try {
                    NodeInfo(nodeUrl, connected = true)

                    val nodeApiUrl = if (!nodeUrl.endsWith('/')) "$nodeUrl/" else nodeUrl

                    val nodeInfoApi =
                        ApiServiceManager.buildRetrofitForNode(InfoApi::class.java, nodeApiUrl)

                    val requestStarted = System.currentTimeMillis()
                    val nodeInfo = nodeInfoApi.nodeInfo.execute().body()!!
                    val blockHeight: Long? = nodeInfo.fullHeight?.toLong()
                    val restApiUrl: String? = nodeInfo.restApiUrl
                    val isExplorer: Boolean = nodeInfo.isExplorer
                    val requestEnded = System.currentTimeMillis()

                    NodeInfo(
                        restApiUrl ?: nodeUrl,
                        true,
                        requestEnded - requestStarted,
                        blockHeight ?: 0L,
                        isExplorer,
                    )
                } catch (t: Throwable) {
                    NodeInfo(nodeUrl, connected = false)
                }
            } else {
                NodeInfo(nodeUrl, connected = false)
            }
        }

        lastNodeList = nodeInfo.filter { it.connected }
            .sortedWith(compareByDescending<NodeInfo> { it.blockHeight }.thenBy { it.responseTime })
        _checkNodesState.value = CheckNodesState.Waiting
    }

    data class NodeInfo(
        val nodeUrl: String,
        val connected: Boolean,
        val responseTime: Long = 0,
        val blockHeight: Long = 0,
        val isExplorer: Boolean = false,
    )

    sealed class CheckNodesState {
        object Waiting : CheckNodesState()
        object FetchingNodes : CheckNodesState()
        data class TestingNode(val nodeUrl: String) : CheckNodesState()
    }
}