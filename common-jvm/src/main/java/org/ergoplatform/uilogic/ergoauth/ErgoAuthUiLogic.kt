package org.ergoplatform.uilogic.ergoauth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.ergoplatform.ergoauth.ErgoAuthRequest
import org.ergoplatform.ergoauth.getErgoAuthRequest
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.getMessageOrName

abstract class ErgoAuthUiLogic {
    abstract val coroutineScope: CoroutineScope

    private var requestJob: Job? = null
    var lastMessage: String? = null
        private set
    var lastMessageSeverity = MessageSeverity.NONE
        private set
    var ergAuthRequest: ErgoAuthRequest? = null
        private set
    private var responseJob: Job? = null

    fun init(ergoAuthUrl: String, texts: StringProvider) {
        if (requestJob == null) {
            requestJob = coroutineScope.launch(Dispatchers.IO) {
                try {
                    val ergoAuthRequest = getErgoAuthRequest(ergoAuthUrl)

                    if (ergoAuthRequest.sigmaBoolean == null || ergoAuthRequest.signingMessage == null
                        || ergoAuthRequest.replyToUrl == null
                    ) {
                        lastMessage = ergoAuthRequest.userMessage?.let {
                            texts.getString(
                                STRING_LABEL_MESSAGE_FROM_DAPP, it
                            )
                        } ?: texts.getString(
                            STRING_LABEL_ERROR_OCCURED,
                            "-"
                        )
                        lastMessageSeverity = MessageSeverity.ERROR
                    } else {
                        ergAuthRequest = ergoAuthRequest
                        lastMessage = ergoAuthRequest.userMessage
                        lastMessageSeverity = ergoAuthRequest.messageSeverity
                    }

                } catch (t: Throwable) {
                    LogUtils.logDebug(
                        this.javaClass.simpleName,
                        "Error fetching ErgoAuthRequest",
                        t
                    )
                    lastMessage = texts.getString(STRING_LABEL_ERROR_OCCURED, t.getMessageOrName())
                    lastMessageSeverity = MessageSeverity.ERROR
                }

                notifyAuthRequestFetched()
            }
        }
    }

    fun getDoneMessage(texts: StringProvider): String =
        lastMessage ?: texts.getString(STRING_DESC_AUTH_RESPONSE_SEND)

    fun getDoneSeverity(): MessageSeverity = lastMessageSeverity

    abstract fun notifyAuthRequestFetched()
}