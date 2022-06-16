package org.ergoplatform.ergoauth

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.appkit.SigmaProp
import org.ergoplatform.transactions.*
import org.ergoplatform.utils.*

private const val uriSchemePrefix = "ergoauth://"

fun isErgoAuthRequest(uri: String) = uri.startsWith(uriSchemePrefix, true)

fun getErgoAuthRequest(ergoAuthUrl: String): ErgoAuthRequest {
    val httpUrl = (if (isLocalOrIpAddress(ergoAuthUrl)) "http://" else "https://") +
            ergoAuthUrl.substringAfter(uriSchemePrefix)

    val jsonResponse = fetchHttpGetStringSync(httpUrl, 30)
    return parseErgoAuthRequestFromJson(jsonResponse)
}

fun postErgoAuthResponse(replyUrl: String, authResponse: ErgoAuthResponse) {
    httpPostStringSync(replyUrl, authResponse.toJson(), MEDIA_TYPE_JSON)
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

private const val JSON_KEY_PROOF= "proof"
private const val JSON_KEY_SIGNEDMESSAGE = "signedMessage"

data class ErgoAuthResponse(
    val signedMessage: String,
    val proof: ByteArray
) {
    fun toJson(): String {
        val gson = GsonBuilder().disableHtmlEscaping().create()
        val root = JsonObject()
        root.addProperty(JSON_KEY_SIGNEDMESSAGE, signedMessage)
        root.addProperty(JSON_KEY_PROOF, String(Base64Coder.encode(proof)))
        return gson.toJson(root)
    }
}