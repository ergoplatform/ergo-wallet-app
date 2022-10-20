package org.ergoplatform.mosaik

import org.ergoplatform.utils.isLocalOrIpAddress

private const val uriSchemePrefix = "mosaikapp://"

fun isMosaikAppUri(uri: String) = uri.startsWith(uriSchemePrefix, true)

fun getMosaikAppHttpUrl(mosaikAppUri: String): String {
    val httpProtocolPrefix = if (isLocalOrIpAddress(mosaikAppUri)) "http://" else "https://"
    return httpProtocolPrefix + mosaikAppUri.substringAfter(uriSchemePrefix)
}