package org.ergoplatform

import org.ergoplatform.transactions.*
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

fun parseErgoPaySigningRequestFromUri(uri: String): ErgoPaySigningRequest {
    if (!isErgoPaySigningRequest(uri)) {
        throw IllegalArgumentException("No ergopay URI provided.")
    }

    val uriWithoutPrefix = uri.substring(uriSchemePrefix.length)
    val reducedTx = Base64Coder.decode(uriWithoutPrefix, true)

    return ErgoPaySigningRequest(reducedTx)
}

/**
 * builds transaction info from Ergo Pay Signing Request, fetches necessary boxes data
 * call this only from non-UI thread and within an applicable try/catch
 */
fun ErgoPaySigningRequest.buildTransactionInfo(ergoApiService: ErgoApiService): TransactionInfo {
    val unsignedTx = deserializeUnsignedTxOffline(reducedTx!!)

    val inputsMap = HashMap<String, TransactionInfoBox>()
    unsignedTx.getInputBoxesIds().forEach {
        val boxInfo = ergoApiService.getBoxInformation(it).execute().body()!!
        inputsMap.put(boxInfo.boxId, boxInfo.toTransactionInfoBox())
    }

    return unsignedTx.buildTransactionInfo(inputsMap)
}

