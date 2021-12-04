package org.ergoplatform.ios.settings

import org.ergoplatform.getDefaultExplorerApiUrl
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_BUTTON_CONNECTION_SETTINGS
import org.ergoplatform.uilogic.STRING_BUTTON_RESET_DEFAULTS
import org.ergoplatform.uilogic.STRING_LABEL_EXPLORER_API_URL
import org.ergoplatform.uilogic.STRING_LABEL_NODE_URL
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class ConnectionSettingsViewController : ViewControllerWithKeyboardLayoutGuide() {

    private lateinit var explorerApiInput: UITextField
    private lateinit var nodeApiInput: UITextField

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
            dismissViewController(true) {}
        }

        val explorerApiLabel = Body1Label().apply {
            text = texts.get(STRING_LABEL_EXPLORER_API_URL)
        }
        explorerApiInput = createTextField().apply {
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

        nodeApiInput = createTextField().apply {
            returnKeyType = UIReturnKeyType.Done
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    nodeApiInput.resignFirstResponder()
                    return true
                }
            }
            text = prefs.prefNodeUrl
            clearButtonMode = UITextFieldViewMode.Always
        }

        val buttonContainer = UIView(CGRect.Zero())
        val defaultsButton = TextButton(texts.get(STRING_BUTTON_RESET_DEFAULTS)).apply {
            addOnTouchUpInsideListener { _, _ ->
                nodeApiInput.text = prefs.getDefaultNodeApiUrl()
                explorerApiInput.text = getDefaultExplorerApiUrl()
            }
        }
        buttonContainer.addSubview(defaultsButton)
        defaultsButton.topToSuperview().bottomToSuperview().rightToSuperview()

        val stackview =
            UIStackView(
                NSArray(
                    explorerApiLabel,
                    explorerApiInput,
                    nodeApiLabel,
                    nodeApiInput,
                    buttonContainer
                )
            ).apply {
                axis = UILayoutConstraintAxis.Vertical
                layoutMargins = UIEdgeInsets(DEFAULT_MARGIN, 0.0, 0.0, 0.0)
                isLayoutMarginsRelativeArrangement = true
                setCustomSpacing(DEFAULT_MARGIN * 2, explorerApiInput)
            }

        val scrollView = stackview.wrapInVerticalScrollView()
        scrollView.setDelaysContentTouches(false)

        view.addSubview(scrollView)
        scrollView.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToKeyboard(this, DEFAULT_MARGIN)
    }
}