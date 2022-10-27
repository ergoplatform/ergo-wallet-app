package org.ergoplatform.uilogic.ergoauth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.ergoplatform.SigningSecrets
import org.ergoplatform.api.PasswordGenerator
import org.ergoplatform.ergoauth.*
import org.ergoplatform.getErrorMessage
import org.ergoplatform.persistance.IAppDatabase
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.signMessage
import org.ergoplatform.transactions.*
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.SigningPromptDialogDataSource
import org.ergoplatform.utils.LogUtils
import org.ergoplatform.utils.getMessageOrName
import org.ergoplatform.wallet.addresses.ensureWalletAddressListHasFirstAddress

abstract class ErgoAuthUiLogic {
    abstract val coroutineScope: CoroutineScope

    private var requestJob: Job? = null
    var lastMessage: String? = null
        private set
    var lastMessageSeverity = MessageSeverity.NONE
        private set
    var ergAuthRequest: ErgoAuthRequest? = null
        private set
    var walletConfig: WalletConfig? = null
    private var walletAddresses: List<WalletAddress> = emptyList()
    private var responseJob: Job? = null

    // are we on the cold device
    var isColdAuth: Boolean = false
        private set

    // the qr pages collector for scanning the request on the cold device
    var requestPagesCollector: QrCodePagesCollector? = null
        private set
    var coldSerializedAuthResponse: String? = null
        private set

    val signingPromptDialogConfig: SigningPromptDialogDataSource by lazy {
        SigningPromptConfig(ergAuthRequest!!.toColdAuthRequest())
    }

    fun init(ergoAuthData: String, walletId: Int, texts: StringProvider, db: IAppDatabase) {
        if (requestJob == null) {
            requestJob = coroutineScope.launch(Dispatchers.IO) {
                try {
                    notifyStateChanged(State.FETCHING_DATA)
                    walletConfig = if (walletId >= 0) {
                        db.walletDbProvider.loadWalletConfigById(walletId)
                    } else {
                        db.walletDbProvider.getAllWalletConfigsSynchronous().minByOrNull {
                            it.displayName?.lowercase() ?: ""
                        }
                    }

                    if (walletConfig == null) {
                        throw IllegalStateException(texts.getString(STRING_ERROR_NO_WALLET))
                    }

                    val firstAddress = walletConfig?.firstAddress!!
                    walletAddresses = ensureWalletAddressListHasFirstAddress(
                        db.walletDbProvider.loadWalletAddresses(firstAddress),
                        firstAddress
                    )

                    isColdAuth = !isErgoAuthRequestUri(ergoAuthData)
                    if (isColdAuth) {
                        requestPagesCollector = QrCodePagesCollector(::getErgoAuthRequestChunk)
                        addRequestQrPage(ergoAuthData, texts)
                        if (requestPagesCollector?.hasAllPages() == false)
                            notifyStateChanged(State.SCANNING)
                    } else {
                        val ergoAuthRequest = getErgoAuthRequest(ergoAuthData)
                        onAuthRequestAvailable(ergoAuthRequest, texts)
                    }

                } catch (t: Throwable) {
                    LogUtils.logDebug(
                        this.javaClass.simpleName,
                        "Error fetching ErgoAuthRequest",
                        t
                    )
                    lastMessage = texts.getString(STRING_LABEL_ERROR_OCCURED, t.getMessageOrName())
                    lastMessageSeverity = MessageSeverity.ERROR
                    notifyStateChanged(State.DONE)
                }
            }
        }
    }

    fun addRequestQrPage(ergoAuthData: String, texts: StringProvider) {
        val added = requestPagesCollector?.addPage(ergoAuthData) ?: false
        lastMessage = if (added) null else texts.getString(STRING_ERROR_COLD_QR_CODE_DOES_NOT_FIT)

        if (requestPagesCollector?.hasAllPages() == true) {
            try {
                onAuthRequestAvailable(
                    ergoAuthRequestFromQrChunks(requestPagesCollector!!.getAllPages()),
                    texts
                )
            } catch (t: Throwable) {
                lastMessage = texts.getString(STRING_LABEL_ERROR_OCCURED, t.getMessageOrName())
                lastMessageSeverity = MessageSeverity.ERROR
                notifyStateChanged(State.DONE)
            }
        }
    }

    private fun onAuthRequestAvailable(ergoAuthRequest: ErgoAuthRequest, texts: StringProvider) {
        if (ergoAuthRequest.sigmaBoolean == null || ergoAuthRequest.signingMessage == null
            || ergoAuthRequest.replyToUrl == null && !isColdAuth
        ) {
            lastMessage = ergoAuthRequest.userMessage?.let {
                texts.getString(STRING_LABEL_MESSAGE_FROM_DAPP, it)
            } ?: texts.getString(STRING_LABEL_ERROR_OCCURED, "-")
            lastMessageSeverity = MessageSeverity.ERROR
            notifyStateChanged(State.DONE)
        } else {
            ergAuthRequest = ergoAuthRequest
            lastMessage = ergoAuthRequest.userMessage
            lastMessageSeverity = ergoAuthRequest.messageSeverity
            notifyStateChanged(State.WAIT_FOR_AUTH)
        }
    }

    fun startResponseFromCold(texts: StringProvider) {
        val ergAuthRequest = this.ergAuthRequest!!
        coroutineScope.launch(Dispatchers.IO) {
            try {
                notifyStateChanged(State.FETCHING_DATA)
                postErgoAuthResponse(
                    ergAuthRequest.replyToUrl!!,
                    ergoAuthResponseFromQrChunks(signingPromptDialogConfig.responsePagesCollector.getAllPages())
                )

                lastMessage = null
                lastMessageSeverity = MessageSeverity.INFORMATION

            } catch (t: Throwable) {
                LogUtils.logDebug(this.javaClass.simpleName, "Error on auth response", t)
                lastMessage = texts.getString(STRING_LABEL_ERROR_OCCURED, getErrorMessage(t, texts))
                lastMessageSeverity = MessageSeverity.ERROR
            }
            notifyStateChanged(State.DONE)
        }
    }

    fun startResponse(secrets: SigningSecrets, texts: StringProvider) {
        val ergAuthRequest = this.ergAuthRequest!!
        coroutineScope.launch(Dispatchers.IO) {
            try {

                notifyStateChanged(State.FETCHING_DATA)

                val prefix = PasswordGenerator.generatePassword(20)
                val suffix = PasswordGenerator.generatePassword(20)
                val signedMessage =
                    prefix + ergAuthRequest.signingMessage + ergAuthRequest.requestHost + suffix
                val signature = signMessage(
                    secrets,
                    walletAddresses.map { it.derivationIndex },
                    ergAuthRequest.sigmaBoolean!!,
                    signedMessage
                )
                val ergoAuthResponse = ErgoAuthResponse(signedMessage, signature)

                if (isColdAuth) {
                    coldSerializedAuthResponse = ergoAuthResponse.toJson()
                } else {
                    postErgoAuthResponse(ergAuthRequest.replyToUrl!!, ergoAuthResponse)
                }

                lastMessage = null
                lastMessageSeverity = MessageSeverity.INFORMATION

            } catch (t: Throwable) {
                LogUtils.logDebug(this.javaClass.simpleName, "Error on auth response", t)
                lastMessage = texts.getString(STRING_LABEL_ERROR_OCCURED, getErrorMessage(t, texts))
                lastMessageSeverity = MessageSeverity.ERROR
            }
            secrets.clearMemory()
            notifyStateChanged(State.DONE)
        }
    }

    fun getAuthenticationMessage(texts: StringProvider): String {
        val authReason =
            getErgoAuthReason(ergAuthRequest!!) ?: texts.getString(STRING_ERROR_NO_AUTH_REASON)
        val introMessage = if (isColdAuth) {
            texts.getString(STRING_INTRO_COLD_AUTH_REQUEST, authReason)
        } else {
            val secConnectionMessage = ergAuthRequest?.sslValidatedBy?.let {
                texts.getString(STRING_DESC_SECURE_CONN, it)
            } ?: texts.getString(STRING_DESC_INSECURE_CONN)
            texts.getString(
                STRING_INTRO_AUTH_REQUEST,
                ergAuthRequest!!.requestHost + " ($secConnectionMessage)",
                authReason
            )
        }

        return introMessage + (ergAuthRequest?.userMessage?.let {
            "\n\n" + texts.getString(
                STRING_LABEL_MESSAGE_FROM_DAPP,
                it
            )
        } ?: "")
    }

    fun getDoneMessage(texts: StringProvider): String =
        lastMessage ?: texts.getString(STRING_DESC_AUTH_RESPONSE_SEND)

    fun getDoneSeverity(): MessageSeverity = lastMessageSeverity

    abstract fun notifyStateChanged(newState: State)

    enum class State {
        FETCHING_DATA,
        SCANNING, // on cold wallet only: waiting to scan more qr chunks
        WAIT_FOR_AUTH,
        DONE
    }

    private inner class SigningPromptConfig(override val signingPromptData: String) :
        SigningPromptDialogDataSource {
        // the qr pages collector for scanning the request on the hot device
        override val responsePagesCollector = QrCodePagesCollector(::getErgoAuthResponseChunk)

        override fun signingRequestToQrChunks(
            serializedSigningRequest: String,
            sizeLimit: Int
        ): List<String> = ergoAuthRequestToQrChunks(serializedSigningRequest, sizeLimit)

        override val lastPageButtonLabel: String
            get() = STRING_BUTTON_SCAN_SIGNED_MSG
        override val descriptionLabel: String
            get() = STRING_DESC_PROMPT_COLD_AUTH_MULTIPLE
        override val lastPageDescriptionLabel: String
            get() = STRING_DESC_PROMPT_COLD_AUTH
    }
}