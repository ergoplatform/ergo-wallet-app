package org.ergoplatform.ios.wallet.addresses

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.getAddressDerivationPath
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.wallet.addresses.WalletAddressDialogUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

/**
 * Wallet address detail dialog to edit an address label or delete the address
 */
class WalletAddressDialogViewController(val address: WalletAddress) : ViewControllerWithKeyboardLayoutGuide() {

    private lateinit var nameInputField: UITextField
    private lateinit var nameChangeApplyButton: UIButton

    private val uiLogic = WalletAddressDialogUiLogic()

    override fun viewDidLoad() {
        super.viewDidLoad()
        val texts = getAppDelegate().texts

        view.backgroundColor = UIColor.systemBackground()
        val closeButton = addCloseButton()

        val addressLabel = Headline2Label().apply {
            text = address.publicAddress
            isUserInteractionEnabled = true

        }
        addressLabel.addGestureRecognizer(UITapGestureRecognizer {
            shareText(address.publicAddress, addressLabel)
        })

        val derivationPathLabel = Body1BoldLabel().apply {
            text = getAddressDerivationPath(address.derivationIndex)
        }

        val nameDescription = Body1Label()
        nameDescription.text = texts.get(STRING_DESC_WALLET_ADDR_LABEL)

        nameInputField = createTextField().apply {
            returnKeyType = UIReturnKeyType.Done
            clearButtonMode = UITextFieldViewMode.Always
            placeholder = texts.get(STRING_HINT_WALLET_ADDR_LABEL)
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    doSaveAddressLabel()
                    return true
                }
            }
            text = address.label ?: ""
        }

        nameChangeApplyButton = TextButton(texts.get(STRING_BUTTON_APPLY))
        nameChangeApplyButton.isEnabled = false
        nameInputField.addOnEditingChangedListener { nameChangeApplyButton.isEnabled = true }

        val nameChangeApplyButtonContainer = UIView(CGRect.Zero())
        nameChangeApplyButtonContainer.addSubview(nameChangeApplyButton)
        nameChangeApplyButton.topToSuperview().bottomToSuperview().rightToSuperview()
        nameChangeApplyButton.addOnTouchUpInsideListener { _, _ -> doSaveAddressLabel() }

        val removeAddressDesc = Body1Label().apply {
            text = texts.get(STRING_DESC_WALLET_ADDR_REMOVE)
        }

        val removeButton = TextButton(texts.get(STRING_LABEL_REMOVE))
        removeButton.addOnTouchUpInsideListener { _, _ ->
            doRemoveAddress()
        }

        val removeButtonContainer = UIView(CGRect.Zero())
        removeButtonContainer.addSubview(removeButton)
        removeButton.topToSuperview().bottomToSuperview().rightToSuperview()

        val stackView = UIStackView(
            NSArray(
                addressLabel,
                derivationPathLabel,
                nameDescription,
                nameInputField,
                nameChangeApplyButtonContainer,
                removeAddressDesc,
                removeButtonContainer
            )
        ).apply {
            axis = UILayoutConstraintAxis.Vertical
            spacing = DEFAULT_MARGIN

            setCustomSpacing(DEFAULT_MARGIN * 3, derivationPathLabel)
            setCustomSpacing(DEFAULT_MARGIN * 2, nameChangeApplyButtonContainer)
        }

        val scrollView = stackView.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.widthMatchesSuperview(false, DEFAULT_MARGIN, MAX_WIDTH)
            .topToBottomOf(closeButton, DEFAULT_MARGIN * 2)
            .bottomToKeyboard(this, DEFAULT_MARGIN)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun doRemoveAddress() {
        GlobalScope.launch {
            uiLogic.deleteWalletAddress(getAppDelegate().database.walletDbProvider, address.id)
        }
        dismissViewController(true) {}
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun doSaveAddressLabel() {
        GlobalScope.launch {
            uiLogic.saveWalletAddressLabel(getAppDelegate().database.walletDbProvider, address.id, nameInputField.text)
        }
        dismissViewController(true) {}
    }
}