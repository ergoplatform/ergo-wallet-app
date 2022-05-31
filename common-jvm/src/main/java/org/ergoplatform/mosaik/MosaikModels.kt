package org.ergoplatform.mosaik

/**
 * Visited Mosaik app to show in last/favorite lists
 */
data class MosaikAppEntry(
    val url: String,
    val name: String,
    val description: String?,
    val icon: ByteArray?,
    val lastVisited: Long,
    val favorite: Boolean,
)

/**
 * Mosaik App hosts share the same GUID that is transferred to the app
 * in order to let different apps on the same servers share their settings
 */
data class MosaikAppHost(
    val hostName: String,
    val guid: String,
)