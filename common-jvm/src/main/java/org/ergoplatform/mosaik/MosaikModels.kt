package org.ergoplatform.mosaik

/**
 * Visited Mosaik app to show in last/favorite lists
 */
data class MosaikAppEntry(
    val url: String,
    val name: String,
    val description: String?,
    val iconFile: String?,
    val lastVisited: Long,
    val favorite: Boolean,
    val notificationUrl: String?,
    val lastNotificationMessage: String? = null,
    val lastNotificationMs: Long = 0,
    val nextNotificationCheck: Long = 0,
    val notificationUnread: Boolean = false,
)

/**
 * Mosaik App hosts share the same GUID that is transferred to the app
 * in order to let different apps on the same servers share their settings
 */
data class MosaikAppHost(
    val hostName: String,
    val guid: String,
)