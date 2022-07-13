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
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.uilogic.*
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

    fun init(ergoAuthUrl: String, walletId: Int, texts: StringProvider, db: IAppDatabase) {
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

                notifyStateChanged(
                    if (ergAuthRequest == null) State.DONE
                    else State.WAIT_FOR_AUTH
                )
            }
        }
    }

    fun startResponse(secrets: SigningSecrets, texts: StringProvider) {
        val ergAuthRequest = this.ergAuthRequest!!
        coroutineScope.launch(Dispatchers.IO) {
            try {

                notifyStateChanged(State.FETCHING_DATA)

                val prefix = PasswordGenerator.generatePassword(20)
                val suffix = PasswordGenerator.generatePassword(20)
                val signedMessage = prefix + ergAuthRequest.signingMessage + ergAuthRequest.requestHost + suffix
                val signature = signMessage(
                    secrets,
                    walletAddresses.map { it.derivationIndex },
                    ergAuthRequest.sigmaBoolean!!,
                    signedMessage
                )

                postErgoAuthResponse(
                    ergAuthRequest.replyToUrl!!,
                    ErgoAuthResponse(signedMessage, signature)
                )
                lastMessage = null
                lastMessageSeverity = MessageSeverity.INFORMATION

            } catch (t: Throwable) {
                lastMessage = texts.getString(STRING_LABEL_ERROR_OCCURED, getErrorMessage(t, texts))
                lastMessageSeverity = MessageSeverity.ERROR
            }
            secrets.clearMemory()
            notifyStateChanged(State.DONE)
        }
    }

    fun getAuthenticationMessage(texts: StringProvider): String {
        return texts.getString(
            STRING_INTRO_AUTH_REQUEST,
            ergAuthRequest!!.requestHost,
            getErgoAuthReason(ergAuthRequest!!) ?: texts.getString(
                STRING_ERROR_NO_AUTH_REASON
            )
        ) + (ergAuthRequest?.userMessage?.let {
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
        WAIT_FOR_AUTH,
        DONE
    }
}