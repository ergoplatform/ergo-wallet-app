package org.ergoplatform

import org.ergoplatform.utils.Base64Coder

/**
 * EIP-0020 ErgoPaySigningRequest
 * everything is optional, but it should have either reducedTx or message
 */
data class ErgoPaySigningRequest(
    val reducedTx: ByteArray?,
    val p2pkAddress: String? = null,
    val message: String? = null,
    val messageSeverity: MessageSeverity = MessageSeverity.NONE,
    val replyToUrl: String? = null
)

enum class MessageSeverity { NONE, INFORMATION, WARNING, ERROR }

private const val uriSchemePrefix = "ergopay:"

fun isErgoPaySigningRequest(uri: String): Boolean {
    return uri.startsWith(uriSchemePrefix, true)
}

fun parseErgoPaySigningRequestFromUri(uri: String): ErgoPaySigningRequest? {
    if (!isErgoPaySigningRequest(uri)) {
        return null
    }

    val uriWithoutPrefix = uri.substring(uriSchemePrefix.length)
    val reducedTx = try {
        Base64Coder.decode(uriWithoutPrefix, true)
    } catch (t: Throwable) {
        null
    }

    return reducedTx?.let { ErgoPaySigningRequest(reducedTx = it) }
}
