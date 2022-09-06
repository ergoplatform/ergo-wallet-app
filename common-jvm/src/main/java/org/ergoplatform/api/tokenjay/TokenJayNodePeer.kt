package org.ergoplatform.api.tokenjay

class TokenJayNodePeer(
        val id: Long,
        val url: String,
        val lastSeen: Long,
        val responseTime: Long,
        val blockHeight: Long,
        val headerHeight: Long,
        val name: String,
    )