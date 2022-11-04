package org.ergoplatform.ergoauth

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.ergoplatform.appkit.SigmaProp
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.utils.*

private const val uriSchemePrefix = "ergoauth://"

fun isErgoAuthRequestUri(uri: String) = uri.startsWith(uriSchemePrefix, true)

fun getErgoAuthRequest(ergoAuthUrl: String): ErgoAuthRequest {
    val httpProtocolPrefix = if (isLocalOrIpAddress(ergoAuthUrl)) "http://" else "https://"
    val httpUrl = httpProtocolPrefix + ergoAuthUrl.substringAfter(uriSchemePrefix)

    val httpsResponse = fetchHttpsGetStringSync(httpUrl, 30)
    val requestHost =
        httpProtocolPrefix + ergoAuthUrl.substringAfter(uriSchemePrefix).substringBefore('/')
    val ergoAuthRequest = parseErgoAuthRequestFromJson(
        httpsResponse.first, requestHost,
        httpsResponse.second?.firstOrNull()?.getIssuerOrg()
    )

    // check if reply to url matches our http url

    if (ergoAuthRequest.replyToUrl?.take(requestHost.length + 1) != requestHost + "/") {
        throw IllegalStateException("ErgoAuth reply URL not on host $requestHost")
    }

    return ergoAuthRequest
}

fun postErgoAuthResponse(replyUrl: String, authResponse: ErgoAuthResponse) {
    httpPostStringSync(replyUrl, authResponse.toJson(), MEDIA_TYPE_JSON)
}

private const val JSON_KEY_SIGMABOOLEAN = "sigmaBoolean"
private const val JSON_KEY_SIGNINGMESSAGE = "signingMessage"
private const val JSON_KEY_USERMESSAGE = "userMessage"
private const val JSON_KEY_MESSAGE_SEVERITY = "messageSeverity"
private const val JSON_KEY_REPLY_TO = "replyTo"

fun parseErgoAuthRequestFromJson(
    jsonString: String,
    requestHost: String,
    sslValidatedBy: String?
): ErgoAuthRequest {
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
        jsonObject.get(JSON_KEY_REPLY_TO)?.asString,
        requestHost,
        sslValidatedBy
    )
}

data class ErgoAuthRequest(
    val signingMessage: String?,
    val sigmaBoolean: SigmaProp?,
    val userMessage: String?,
    val messageSeverity: MessageSeverity = MessageSeverity.NONE,
    val replyToUrl: String? = null,
    val requestHost: String,
    val sslValidatedBy: String?
) {
    fun toColdAuthRequest(): String {
        val gson = GsonBuilder().disableHtmlEscaping().create()
        val root = JsonObject()
        signingMessage?.let { root.addProperty(JSON_KEY_SIGNINGMESSAGE, signingMessage) }
        sigmaBoolean?.let {
            root.addProperty(
                JSON_KEY_SIGMABOOLEAN,
                String(Base64Coder.encode(sigmaBoolean.toBytes()))
            )
        }
        userMessage?.let { root.addProperty(JSON_KEY_USERMESSAGE, userMessage) }
        root.addProperty(JSON_KEY_MESSAGE_SEVERITY, messageSeverity.toString())
        return gson.toJson(root)
    }
}

fun getErgoAuthReason(ergoAuthRequest: ErgoAuthRequest): String? {
    return try {
        val signingMessage = ergoAuthRequest.signingMessage

        val reason = signingMessage?.substringBefore('\u0000', "")
        return if (reason.isNullOrEmpty()) null else reason
    } catch (t: Throwable) {
        null
    }
}

private const val JSON_KEY_PROOF = "proof"
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

    companion object {
        fun fromJson(json: String): ErgoAuthResponse {
            val jsonTree = JsonParser().parse(json) as JsonObject
            return ErgoAuthResponse(
                jsonTree.get(JSON_KEY_SIGNEDMESSAGE).asString,
                Base64Coder.decode(jsonTree.get(JSON_KEY_PROOF).asString, false)
            )
        }
    }
}