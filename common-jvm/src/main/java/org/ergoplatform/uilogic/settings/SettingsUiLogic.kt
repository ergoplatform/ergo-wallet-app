package org.ergoplatform.uilogic.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.persistance.PreferencesProvider
import org.ergoplatform.refreshNodeList
import org.ergoplatform.restapi.client.InfoApi
import org.ergoplatform.uilogic.STRING_BUTTON_DISPLAY_CURRENCY
import org.ergoplatform.uilogic.STRING_LABEL_NONE
import org.ergoplatform.uilogic.StringProvider
import java.util.*
import kotlin.coroutines.coroutineContext

class SettingsUiLogic {
    fun getFiatCurrencyButtonText(preferences: PreferencesProvider, texts: StringProvider): String {
        val displayCurrency =
            preferences.prefDisplayCurrency.uppercase(Locale.getDefault())
        return texts.getString(
            STRING_BUTTON_DISPLAY_CURRENCY,
            if (displayCurrency.isNotEmpty()) displayCurrency else texts.getString(STRING_LABEL_NONE)
        )
    }

    val checkNodesState: StateFlow<CheckNodesState> get() = _checkNodesState
    private val _checkNodesState = MutableStateFlow<CheckNodesState>(CheckNodesState.Waiting)

    suspend fun checkAvailableNodes(prefs: PreferencesProvider): List<NodeInfo> {
        var knownNodesList = prefs.knownNodesList

        if (knownNodesList.isEmpty()) {
            // try to connect to current node to fetch the list
            _checkNodesState.value = CheckNodesState.FetchingNodes
            refreshNodeList(prefs)
            knownNodesList = prefs.knownNodesList

            if (coroutineContext.isActive) {
                // TODO #143 if still empty, connect TokenJay and fetch the list
            }
        }

        knownNodesList = mutableListOf(prefs.prefNodeUrl).apply { addAll(knownNodesList) }

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
                    val requestEnded = System.currentTimeMillis()

                    NodeInfo(
                        restApiUrl ?: nodeUrl,
                        true,
                        requestEnded - requestStarted,
                        blockHeight ?: 0L
                    )
                } catch (t: Throwable) {
                    NodeInfo(nodeUrl, connected = false)
                }
            } else {
                NodeInfo(nodeUrl, connected = false)
            }
        }

        _checkNodesState.value = CheckNodesState.Waiting
        return nodeInfo.filter { it.connected }
            .sortedWith(compareByDescending<NodeInfo> { it.blockHeight }.thenBy { it.responseTime })
    }

    data class NodeInfo(
        val nodeUrl: String,
        val connected: Boolean,
        val responseTime: Long = 0,
        val blockHeight: Long = 0,
    )

    sealed class CheckNodesState {
        object Waiting : CheckNodesState()
        object FetchingNodes : CheckNodesState()
        data class TestingNode(val nodeUrl: String) : CheckNodesState()
    }
}