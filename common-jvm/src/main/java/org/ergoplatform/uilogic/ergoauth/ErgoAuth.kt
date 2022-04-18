package org.ergoplatform.ergoauth

import com.google.gson.JsonParser
import org.ergoplatform.appkit.SigmaProp
import org.ergoplatform.transactions.*
import org.ergoplatform.utils.Base64Coder
import org.ergoplatform.utils.fetchHttpGetStringSync
import org.ergoplatform.utils.isLocalOrIpAddress

private const val uriSchemePrefix = "ergoauth://"

fun isErgoAuthRequest(qrCode: String) = qrCode.startsWith(uriSchemePrefix, true)

fun getErgoAuthRequest(ergoAuthUrl: String): ErgoAuthRequest {
    val httpUrl = (if (isLocalOrIpAddress(ergoAuthUrl)) "http://" else "https://") +
            ergoAuthUrl.substringAfter(uriSchemePrefix)

    val jsonResponse = fetchHttpGetStringSync(httpUrl)
    return parseErgoAuthRequestFromJson(jsonResponse)
}

private const val JSON_KEY_SIGMABOOLEAN = "sigmaBoolean"
private const val JSON_KEY_SIGNINGMESSAGE = "signingMessage"
private const val JSON_KEY_USERMESSAGE = "userMessage"
private const val JSON_KEY_MESSAGE_SEVERITY = "messageSeverity"
private const val JSON_KEY_REPLY_TO = "replyTo"

fun parseErgoAuthRequestFromJson(jsonString: String): ErgoAuthRequest {
    val jsonObject = JsonParser().parse(jsonString).asJsonObject
    val sigmaBoolean = jsonObject.get(JSON_KEY_SIGMABOOLEAN)?.asString?.let {
        SigmaProp.parseFromBytes(Base64Coder.decode(it, false))
    }

    return ErgoAuthRequest(
        jsonObject.get(JSON_KEY_SIGNINGMESSAGE)?.asString,
        sigmaBoolean,
        jsonObject.get(JSON_KEY_USERMESSAGE)?.asString,
        jsonObject.get(JSON_KEY_MESSAGE_SEVERITY)?.asString?.let { MessageSeverity.valueOf(it) }
            ?: MessageSeverity.NONE,
        jsonObject.get(JSON_KEY_REPLY_TO)?.asString
    )
}

data class ErgoAuthRequest(
    val signingMessage: String?,
    val sigmaBoolean: SigmaProp?,
    val userMessage: String?,
    val messageSeverity: MessageSeverity = MessageSeverity.NONE,
    val replyToUrl: String? = null
)