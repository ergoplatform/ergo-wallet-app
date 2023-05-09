package org.ergoplatform.ios.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.getDefaultExplorerApiUrl
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.settings.SettingsUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class ConnectionSettingsViewController : ViewControllerWithKeyboardLayoutGuide() {

    private lateinit var explorerApiInput: UITextField
    private lateinit var nodeApiInput: UITextField
    private lateinit var tokenVerificationInput: UITextField
    private lateinit var ipfsGatewayInput: UITextField
    private lateinit var nodeDetectionState: UIStackView
    private lateinit var preferNodeApiSwitch: UISwitch

    private val uiLogic = SettingsUiLogic()
    private var nodeDetectionStarted = false

    private val waitingView = WaitingView()

    override fun viewDidLoad() {
        super.viewDidLoad()

        val appDelegate = getAppDelegate()
        val texts = appDelegate.texts
        val prefs = appDelegate.prefs

        view.backgroundColor = UIColor.systemBackground()
        title = texts.get(STRING_BUTTON_CONNECTION_SETTINGS)

        navigationController.navigationBar?.tintColor = uiColorErgo
        val closeButton = UIBarButtonItem(UIBarButtonSystemItem.Close)
        navigationItem.leftBarButtonItem = closeButton
        closeButton.setOnClickListener { dismissViewController(true) {} }
        val saveButton = UIBarButtonItem(UIBarButtonSystemItem.Save)
        navigationItem.rightBarButtonItem = saveButton
        saveButton.setOnClickListener {
            prefs.prefNodeUrl = nodeApiInput.text
            prefs.prefExplorerApiUrl = explorerApiInput.text
            prefs.prefTokenVerificationUrl = tokenVerificationInput.text
            prefs.prefIpfsGatewayUrl = ipfsGatewayInput.text
            prefs.isPreferNodeExplorer = preferNodeApiSwitch.isOn

            // reset api service of NodeConnector to load new settings
            ApiServiceManager.resetApiService()

            dismissViewController(true) {}
        }

        val explorerApiLabel = Body1Label().apply {
            text = texts.get(STRING_LABEL_EXPLORER_API_URL)
        }
        explorerApiInput = EndIconTextField().apply {
            returnKeyType = UIReturnKeyType.Next
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    nodeApiInput.becomeFirstResponder()
                    return true
                }
            }
            text = prefs.prefExplorerApiUrl
            clearButtonMode = UITextFieldViewMode.Always
        }

        val nodeApiLabel = Body1Label().apply {
            text = texts.get(STRING_LABEL_NODE_URL)
        }

        nodeApiInput = EndIconTextField().apply {
            returnKeyType = UIReturnKeyType.Next
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    tokenVerificationInput.becomeFirstResponder()
                    return true
                }
            }
            text = prefs.prefNodeUrl
            setCustomActionField(
                getIosSystemImage(
                    IMAGE_AUTO_FIX,
                    UIImageSymbolScale.Small
                )!!
            ) {
                viewControllerScope.launch(Dispatchers.IO) {
                    uiLogic.checkAvailableNodes(prefs)
                    nodeDetectionStarted = true
                }
            }
            clearButtonMode = UITextFieldViewMode.WhileEditing
            rightViewMode = UITextFieldViewMode.UnlessEditing
        }

        val preferNodeApiContainer = UiSwitchWithLabel(texts.get(STRING_CHECK_PREFER_NODE))
        preferNodeApiSwitch = preferNodeApiContainer.switch
        preferNodeApiSwitch.isOn = prefs.isPreferNodeExplorer

        val tokenVerificationLabel = Body1Label().apply {
            text = texts.get(STRING_LABEL_TOKEN_VERIFICATION_URL)
        }

        tokenVerificationInput = EndIconTextField().apply {
            returnKeyType = UIReturnKeyType.Next
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    ipfsGatewayInput.becomeFirstResponder()
                    return true
                }
            }
            text = prefs.prefTokenVerificationUrl
            clearButtonMode = UITextFieldViewMode.Always
        }

        val ipfsGatewayTitle = Body1Label().apply {
            text = texts.get(STRING_LABEL_IPFS_HTTP_GATEWAY)
        }
        ipfsGatewayInput = EndIconTextField().apply {
            returnKeyType = UIReturnKeyType.Done
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    textField?.resignFirstResponder()
                    return true
                }
            }
            text = prefs.prefIpfsGatewayUrl
            clearButtonMode = UITextFieldViewMode.Always
        }

        nodeDetectionState = UIStackView(CGRect.Zero()).apply {
            axis = UILayoutConstraintAxis.Vertical
        }

        val buttonContainer = UIView(CGRect.Zero())
        val defaultsButton = TextButton(texts.get(STRING_BUTTON_RESET_DEFAULTS)).apply {
            addOnTouchUpInsideListener { _, _ ->
                nodeApiInput.text = prefs.getDefaultNodeApiUrl()
                explorerApiInput.text = getDefaultExplorerApiUrl()
                tokenVerificationInput.text = prefs.defaultTokenVerificationUrl
                ipfsGatewayInput.text = prefs.defaultIpfsGatewayUrl
                preferNodeApiSwitch.isOn = false
            }
        }
        buttonContainer.addSubview(defaultsButton)
        defaultsButton.topToSuperview().bottomToSuperview().rightToSuperview()

        val stackView =
            UIStackView(
                NSArray(
                    explorerApiLabel,
                    explorerApiInput,
                    nodeApiLabel,
                    nodeApiInput,
                    preferNodeApiContainer,
                    nodeDetectionState,
                    tokenVerificationLabel,
                    tokenVerificationInput,
                    ipfsGatewayTitle,
                    ipfsGatewayInput,
                    buttonContainer
                )
            ).apply {
                axis = UILayoutConstraintAxis.Vertical
                spacing = DEFAULT_MARGIN / 2
                layoutMargins = UIEdgeInsets(DEFAULT_MARGIN, 0.0, 0.0, 0.0)
                isLayoutMarginsRelativeArrangement = true
                setCustomSpacing(DEFAULT_MARGIN * 2, explorerApiInput)
                setCustomSpacing(DEFAULT_MARGIN, nodeApiInput)
                setCustomSpacing(DEFAULT_MARGIN * 2, preferNodeApiContainer)
                setCustomSpacing(DEFAULT_MARGIN, nodeDetectionState)
                setCustomSpacing(DEFAULT_MARGIN * 2, tokenVerificationInput)
            }

        val scrollView = stackView.wrapInVerticalScrollView()
        scrollView.setDelaysContentTouches(false)

        view.addSubview(scrollView)
        scrollView.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToKeyboard(this, DEFAULT_MARGIN)
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)

        viewControllerScope.launch {
            uiLogic.checkNodesState.collect { nodeState ->
                runOnMainThread {
                    updateNodeFetchState(nodeState)
                }
            }
        }
    }

    private fun updateNodeFetchState(nodeState: SettingsUiLogic.CheckNodesState) {
        nodeApiInput.isHidden = nodeState != SettingsUiLogic.CheckNodesState.Waiting

        nodeDetectionState.clearArrangedSubviews()

        val texts = getAppDelegate().texts
        if (nodeState == SettingsUiLogic.CheckNodesState.Waiting) {
            if (uiLogic.lastNodeList.isNotEmpty()) {
                // add detected nodes to list

                nodeDetectionState.addArrangedSubview(Body1Label().apply {
                    text = texts.get(STRING_LABEL_NODE_ALTERNATIVES)
                })

                uiLogic.lastNodeList.forEach {
                    val nodeInfoCard = NodeInfoCard(it, IosStringProvider(texts), onClick = {
                        nodeApiInput.text = it
                        nodeDetectionState.clearArrangedSubviews()
                    })
                    nodeDetectionState.addArrangedSubview(nodeInfoCard)
                }

            } else if (nodeDetectionStarted) {
                // show error message
                val errorLabel = Body1Label().apply {
                    text = texts.get(STRING_LABEL_NODE_NONE_FOUND)
                    textColor = uiColorErgo
                    textAlignment = NSTextAlignment.Center
                }
                nodeDetectionState.addArrangedSubview(errorLabel)
            }
        } else {
            // We are doing something, show waiting UI
            nodeDetectionState.addArrangedSubview(waitingView)

            when (nodeState) {
                SettingsUiLogic.CheckNodesState.FetchingNodes -> waitingView.showWaitingText(
                    texts.get(STRING_LABEL_FETCHING_NODE_LIST)
                )
                is SettingsUiLogic.CheckNodesState.TestingNode -> waitingView.showWaitingText(
                    texts.format(STRING_LABEL_CHECKING_NODE, nodeState.nodeUrl)
                )
                SettingsUiLogic.CheckNodesState.Waiting -> {}
            }
        }
    }

    private class NodeInfoCard(
        private val nodeInfo: SettingsUiLogic.NodeInfo,
        private val stringProvider: IosStringProvider,
        private val onClick: (String) -> Unit
    ) : CardView() {

        private val nodeUrlLabel = Body1Label().apply {
            text = nodeInfo.nodeUrl
            numberOfLines = 1
        }

        private val moreInfoLabel = Body2Label().apply {
            text = stringProvider.getString(
                STRING_LABEL_NODE_INFO,
                nodeInfo.blockHeight,
                nodeInfo.responseTime
            ) + (if (nodeInfo.isExplorer) "\n" + stringProvider.getString(
                STRING_LABEL_NODE_EXPLORER_API
            ) else "")
            textColor = UIColor.secondaryLabel()
        }

        init {
            contentView.apply {
                addSubview(nodeUrlLabel)
                addSubview(moreInfoLabel)

                nodeUrlLabel.topToSuperview().widthMatchesSuperview()
                moreInfoLabel.topToBottomOf(nodeUrlLabel).widthMatchesSuperview().bottomToSuperview()
            }

            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                onClick(nodeInfo.nodeUrl)
            })
        }
    }
}