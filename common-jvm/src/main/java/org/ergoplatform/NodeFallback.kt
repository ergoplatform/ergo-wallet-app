package org.ergoplatform

import java.io.IOException

internal fun buildNodeFallbackList(
    preferredNodeUrl: String,
    knownNodeUrls: List<String>
): List<String> {
    return (listOf(preferredNodeUrl) + knownNodeUrls)
        .mapNotNull { nodeUrl ->
            nodeUrl.trim().trimEnd('/').takeIf { it.isNotEmpty() }?.plus('/')
        }
        .distinct()
}

internal fun <T> executeWithNodeFallback(
    nodeUrls: List<String>,
    action: (String) -> T
): T {
    require(nodeUrls.isNotEmpty()) { "At least one node URL is required" }

    var lastFailure: Throwable? = null
    nodeUrls.forEach { nodeUrl ->
        try {
            return action(nodeUrl)
        } catch (failure: Throwable) {
            if (!failure.isCausedByConnectionError()) {
                throw failure
            }
            lastFailure = failure
        }
    }

    throw checkNotNull(lastFailure)
}

private fun Throwable.isCausedByConnectionError(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is IOException) {
            return true
        }
        current = current.cause
    }
    return false
}
