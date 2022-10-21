package org.ergoplatform.ios.addressbook

import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.AddressBookEntry
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.addressbook.EditAddressEntryUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class EditAddressDialogViewController(
    private val addressBookEntry: AddressBookEntry?,
) : ViewControllerWithKeyboardLayoutGuide() {

    private val uiLogic = IosEditAddressUiLogic()

    init {
        modalPresentationStyle = UIModalPresentationStyle.OverFullScreen
        modalTransitionStyle = UIModalTransitionStyle.CrossDissolve
    }

    private lateinit var inputAddressLabel: EndIconTextField
    private lateinit var inputAddress: EndIconTextField
    private lateinit var titleLabel: Body1BoldLabel
    private lateinit var deleteButton: UIButton

    override fun viewDidLoad() {
        super.viewDidLoad()

        val viewContainer = UIView(CGRect.Zero()).apply {
            this.layer.setMasksToBounds(true)
            this.layer.cornerRadius = 6.0
            this.layer.borderWidth = 1.0
            this.layer.borderColor = UIColor.label().cgColor
            backgroundColor = UIColor.systemBackground()
        }
        view.backgroundColor = UIColor.black().addAlpha(0.5)
        val keyboardView = UIView(CGRect.Zero())
        view.addSubview(keyboardView)
        keyboardView.topToSuperview(true).bottomToKeyboard(this).widthMatchesSuperview(true)
        keyboardView.addSubview(viewContainer)
        viewContainer.centerVertical().widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)

        val stackView = UIStackView().apply {
            axis = UILayoutConstraintAxis.Vertical
            viewContainer.addSubview(this)
            edgesToSuperview(inset = DEFAULT_MARGIN)
            spacing = DEFAULT_MARGIN
        }

        val texts = getAppDelegate().texts

        titleLabel = Body1BoldLabel().apply {
            numberOfLines = 1
            lineBreakMode = NSLineBreakMode.TruncatingTail
            text = texts.get(STRING_BUTTON_ADD_ADDRESS)
            textAlignment = NSTextAlignment.Center
        }
        val closeButton = UIButton(UIButtonType.Close)
        closeButton.addOnTouchUpInsideListener { _, _ -> dismissViewController(true) {} }

        inputAddress = EndIconTextField().apply {
            placeholder = texts.get(STRING_LABEL_ERG_ADDRESS)
            returnKeyType = UIReturnKeyType.Done
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    saveEntry()
                    return true
                }
            }
        }

        inputAddressLabel = EndIconTextField().apply {
            placeholder = texts.get(STRING_LABEL_DESCRIPTIVE_ADDRESS_NAME)
            returnKeyType = UIReturnKeyType.Next
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    inputAddress.becomeFirstResponder()
                    return true
                }
            }
        }

        val buttonBar = UIView(CGRect.Zero()).apply {
            layoutMargins = UIEdgeInsets.Zero()
        }
        deleteButton = CommonButton(texts.get(STRING_BUTTON_DELETE)).apply {
            addOnTouchUpInsideListener { _, _ ->
                uiLogic.deleteAddress(getAppDelegate().database.addressBookDbProvider)
                dismissViewController(true) {}
            }
            isHidden = true
            buttonBar.addSubview(this)
            this.superViewWrapsHeight().leftToSuperview()
        }
        val saveButton = PrimaryButton(texts.get(STRING_BUTTON_SAVE)).apply {
            addOnTouchUpInsideListener { _, _ ->
                saveEntry()
            }
        }
        buttonBar.addSubview(saveButton)
        saveButton.rightToSuperview()

        stackView.addArrangedSubview(titleLabel)
        stackView.setCustomSpacing(DEFAULT_MARGIN * 4, titleLabel)
        stackView.addArrangedSubview(inputAddressLabel)
        stackView.addArrangedSubview(inputAddress)
        stackView.addArrangedSubview(buttonBar)

        viewContainer.addSubview(closeButton)
        closeButton.leftToSuperview(inset = DEFAULT_MARGIN).centerVerticallyTo(titleLabel)
    }

    private fun saveEntry() {
        val response = uiLogic.saveAddressEntry(
            inputAddressLabel.text,
            inputAddress.text,
            getAppDelegate().database.addressBookDbProvider
        )
        if (response.hasSaved)
            dismissViewController(true) {}
        else {
            inputAddress.setHasError(response.addressError)
            inputAddressLabel.setHasError(response.labelError)
        }
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        uiLogic.init(addressBookEntry?.id ?: 0, getAppDelegate().database.addressBookDbProvider)
    }

    inner class IosEditAddressUiLogic : EditAddressEntryUiLogic() {
        override fun coroutineScope(): CoroutineScope =
            viewControllerScope

        override fun notifyNewValue(value: AddressBookEntry) {
            runOnMainThread {
                if (canDeleteAddress) {
                    titleLabel.text = getAppDelegate().texts.get(STRING_BUTTON_EDIT_ADDRESS_ENTRY)
                    deleteButton.isHidden = false
                }

                inputAddress.text = value.address
                inputAddressLabel.text = value.label
            }
        }

    }
}