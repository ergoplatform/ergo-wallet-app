package org.ergoplatform.ios.ergoauth

import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ios.transactions.ErgoPaySigningViewController
import org.ergoplatform.ios.transactions.SigningPromptViewController
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.ChooseWalletViewController
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.transactions.ergoAuthRequestToQrChunks
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.ergoauth.ErgoAuthUiLogic
import org.ergoplatform.wallet.isReadOnly
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class ErgoAuthenticationViewController(
    private val request: String,
    private val walletId: Int?,
    private val doOnComplete: (() -> Unit)? = null,
) : CoroutineViewController() {

    private val uiLogic = IosUiLogic()
    private lateinit var authRequestContainer: AuthenticationRequestStack
    private lateinit var fetchingContainer: ErgoPaySigningViewController.FetchDataContainer
    private lateinit var texts: IosStringProvider
    private val stateDoneContainer = CardView()

    override fun viewDidLoad() {
        super.viewDidLoad()

        val appDelegate = getAppDelegate()
        texts = IosStringProvider(appDelegate.texts)
        fetchingContainer = ErgoPaySigningViewController.FetchDataContainer(texts)
        authRequestContainer = AuthenticationRequestStack()

        title = texts.getString(STRING_TITLE_ERGO_AUTH_REQUEST)
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        view.layoutMargins = UIEdgeInsets.Zero()

        val scrollingContainer = UIView(CGRect.Zero())
        val scrollView = scrollingContainer.wrapInVerticalScrollView()

        view.addSubview(scrollView)
        view.addSubview(fetchingContainer)
        view.addSubview(stateDoneContainer)
        scrollingContainer.addSubview(authRequestContainer)

        fetchingContainer.widthMatchesSuperview(maxWidth = MAX_WIDTH).centerVertical()
        authRequestContainer.edgesToSuperview(maxWidth = MAX_WIDTH)
        stateDoneContainer.centerVertical().widthMatchesSuperview(maxWidth = MAX_WIDTH)
        scrollView.edgesToSuperview()

        uiLogic.init(
            request,
            walletId ?: -1,
            texts,
            appDelegate.database
        )
    }

    private fun showDoneInfo() {
        if (stateDoneContainer.contentView.subviews.isEmpty()) {
            val image = uiLogic.getDoneSeverity().getImage()?.let {
                UIImageView(getIosSystemImage(it, UIImageSymbolScale.Large)).apply {
                    contentMode = UIViewContentMode.ScaleAspectFit
                    tintColor = uiColorErgo
                    fixedHeight(100.0)
                }
            }

            val descLabel = Body1Label()
            descLabel.text = uiLogic.getDoneMessage(texts)
            descLabel.textAlignment = NSTextAlignment.Center

            val dismissButton = PrimaryButton(texts.getString(STRING_BUTTON_DONE))
            dismissButton.addOnTouchUpInsideListener { _, _ ->
                navigationController.popViewController(
                    true
                )
            }
            val doneButtonContainer = UIView()
            doneButtonContainer.addSubview(dismissButton)
            dismissButton.centerHorizontal().topToSuperview().bottomToSuperview().fixedWidth(150.0)

            val txDoneStack = UIStackView().apply {
                axis = UILayoutConstraintAxis.Vertical
                spacing = DEFAULT_MARGIN * 3

                image?.let { addArrangedSubview(image) }
                addArrangedSubview(descLabel)
                addArrangedSubview(doneButtonContainer)
            }

            stateDoneContainer.contentView.addSubview(txDoneStack)
            txDoneStack.edgesToSuperview(inset = DEFAULT_MARGIN * 2)
        }
    }

    inner class AuthenticationRequestStack : UIStackView(CGRect.Zero()) {
        private val messageFromDApp = Body1Label()
        private val messageIcon = UIImageView().apply {
            tintColor = UIColor.secondaryLabel()
            contentMode = UIViewContentMode.ScaleAspectFit
            fixedWidth(40.0)
        }
        private val requestFromDappCard = CardView()
        private val mainRequestContainer = ChooseAddressAndSignContainer()

        init {
            axis = UILayoutConstraintAxis.Vertical
            spacing = DEFAULT_MARGIN

            addArrangedSubview(requestFromDappCard)
            addArrangedSubview(mainRequestContainer)

            requestFromDappCard.contentView.addSubview(messageFromDApp)
            requestFromDappCard.contentView.addSubview(messageIcon)

            val messageStackView = UIStackView(NSArray(messageIcon, messageFromDApp)).apply {
                axis = UILayoutConstraintAxis.Horizontal
                spacing = DEFAULT_MARGIN * 2
            }
            requestFromDappCard.contentView.addSubview(messageStackView)
            messageStackView.edgesToSuperview(inset = DEFAULT_MARGIN)
        }

        fun showRequestInfo() {
            requestFromDappCard.isHidden = false
            messageFromDApp.text = uiLogic.getAuthenticationMessage(texts)
            messageIcon.isHidden = uiLogic.ergAuthRequest!!.messageSeverity.getImage()?.let {
                messageIcon.image = getIosSystemImage(it, UIImageSymbolScale.Medium)
                false
            } ?: true

            mainRequestContainer.refreshAddressInfo()
        }
    }

    inner class ChooseAddressAndSignContainer : UIView(CGRect.Zero()) {
        private val descLabel = Body1Label().apply {
            text = texts.getString(STRING_DESC_AUTHENTICATION_WALLET)
            textAlignment = NSTextAlignment.Center
        }

        private val walletNameLabel = Headline2Label().apply {
            numberOfLines = 1
            textColor = uiColorErgo
        }
        private val authButton = PrimaryButton(texts.getString(STRING_BUTTON_AUTHENTICATE)).apply {
            addOnTouchUpInsideListener { _, _ ->
                uiLogic.walletConfig?.let { walletConfig ->
                    if (!walletConfig.isReadOnly()) {
                        startAuthFlow(walletConfig) { secrets ->
                            uiLogic.startResponse(secrets, texts)
                        }
                    } else {
                        presentViewController(
                            SigningPromptViewController(
                                uiLogic.ergAuthRequest!!.toColdAuthRequest(),
                                responsePagesCollector = { null }, // TODO
                                onSigningPromptResponseScanComplete = { TODO() },
                                signingRequestToChunks = ::ergoAuthRequestToQrChunks,
                                lastPageButtonLabel = STRING_BUTTON_SCAN_SIGNED_MSG,
                                descriptionLabel = STRING_DESC_PROMPT_COLD_AUTH_MULTIPLE,
                                lastPageDescriptionLabel = STRING_DESC_PROMPT_COLD_AUTH,
                            ), true
                        ) {}
                    }
                }
            }
        }

        init {
            val walletChooseButton = walletNameLabel.wrapWithTrailingImage(
                getIosSystemImage(IMAGE_OPEN_LIST, UIImageSymbolScale.Small, 20.0)!!,
                keepWidth = true
            ).apply {
                isUserInteractionEnabled = true
                addGestureRecognizer(UITapGestureRecognizer {
                    presentViewController(
                        ChooseWalletViewController {
                            uiLogic.walletConfig = it
                            refreshAddressInfo()
                        },
                        true
                    ) {}
                })
            }

            addSubview(descLabel)
            addSubview(walletChooseButton)
            addSubview(authButton)

            descLabel.topToSuperview().widthMatchesSuperview()
            walletChooseButton.topToBottomOf(descLabel).centerHorizontal(true)
            authButton.topToBottomOf(walletNameLabel, inset = DEFAULT_MARGIN * 2)
                .bottomToSuperview()
                .centerHorizontal().fixedWidth(200.0)
        }

        fun refreshAddressInfo() {
            walletNameLabel.text = uiLogic.walletConfig?.displayName
        }
    }

    inner class IosUiLogic : ErgoAuthUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewControllerScope

        override fun notifyStateChanged(newState: State) {
            runOnMainThread {
                stateDoneContainer.isHidden = newState != State.DONE
                fetchingContainer.isHidden = newState != State.FETCHING_DATA
                authRequestContainer.isHidden = newState != State.WAIT_FOR_AUTH

                if (newState == State.DONE) {
                    showDoneInfo()

                    if (getDoneSeverity() != MessageSeverity.ERROR) {
                        doOnComplete?.invoke()
                    }
                } else if (newState == State.WAIT_FOR_AUTH) {
                    authRequestContainer.showRequestInfo()
                }
            }
        }
    }
}