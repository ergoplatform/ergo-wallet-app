package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.NodeConnector
import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.TransactionResult
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.ergoplatform.utils.formatFiatToString
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.getNumOfAddresses
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSRange
import org.robovm.apple.uikit.*

class SendFundsViewController(
    val walletId: Int,
    val derivationIdx: Int = -1,
    val paymentRequest: String? = null
) : ViewControllerWithKeyboardLayoutGuide() {
    private lateinit var texts: I18NBundle
    private val uiLogic = IosSendFundsUiLogic()
    private lateinit var scrollView: UIView
    private lateinit var walletTitle: UILabel
    private lateinit var addressNameLabel: UILabel
    private lateinit var balanceLabel: UILabel
    private lateinit var fiatLabel: UILabel

    private lateinit var inputReceiver: UITextField
    private lateinit var inputErgoAmount: UITextField

    override fun viewDidLoad() {
        super.viewDidLoad()

        texts = getAppDelegate().texts
        title = texts.get(STRING_BUTTON_SEND)
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        // TODO qr code scan
        //val uiBarButtonItem = UIBarButtonItem(UIBarButtonSystemItem.Action)
        //uiBarButtonItem.setOnClickListener {
        //}
        //navigationController.topViewController.navigationItem.rightBarButtonItem = uiBarButtonItem

        walletTitle = Body1Label()
        walletTitle.numberOfLines = 1
        addressNameLabel = Body1BoldLabel()
        addressNameLabel.numberOfLines = 1
        addressNameLabel.textColor = uiColorErgo
        balanceLabel = Body1Label()
        balanceLabel.numberOfLines = 1

        val introLabel = Body1Label()
        introLabel.text = texts.get(STRING_DESC_SEND_FUNDS)

        // TODO add cold wallet hint box here

        inputReceiver = createTextField().apply {
            placeholder = texts.get(STRING_LABEL_RECEIVER_ADDRESS)
            returnKeyType = UIReturnKeyType.Next
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    inputErgoAmount.becomeFirstResponder()
                    return true
                }
            }
        }
        inputErgoAmount = createTextField().apply {
            placeholder = texts.get(STRING_LABEL_AMOUNT)
            keyboardType = UIKeyboardType.NumbersAndPunctuation
            returnKeyType = UIReturnKeyType.Next
            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldChangeCharacters(
                    textField: UITextField?,
                    range: NSRange?,
                    string: String?
                ): Boolean {
                    // TODO does not work as intended (allows multiple dots)
                    return string?.matches(Regex("^\\d*\\.?(\\d)*$")) ?: true
                }
                override fun shouldReturn(textField: UITextField?): Boolean {
                    inputErgoAmount.resignFirstResponder()
                    return true
                }
            }
            addOnEditingChangedListener {
                // TODO
            }
        }

        fiatLabel = Body1Label()
        fiatLabel.textAlignment = NSTextAlignment.Right
        fiatLabel.isHidden = true

        val container = UIView()
        val stackView = UIStackView(
            NSArray(
                walletTitle,
                addressNameLabel,
                balanceLabel,
                introLabel,
                inputReceiver,
                inputErgoAmount,
                fiatLabel
            )
        )
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.spacing = 2 * DEFAULT_MARGIN
        stackView.setCustomSpacing(0.0, walletTitle)
        stackView.setCustomSpacing(0.0, addressNameLabel)
        stackView.setCustomSpacing(0.0, inputErgoAmount)
        scrollView = container.wrapInVerticalScrollView()
        container.addSubview(stackView)
        stackView.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)

        view.addSubview(scrollView)
        scrollView.topToSuperview().widthMatchesSuperview().bottomToKeyboard(this)
        scrollView.isHidden = true
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        uiLogic.initWallet(getAppDelegate().database, walletId, derivationIdx, paymentRequest)
    }

    inner class IosSendFundsUiLogic : SendFundsUiLogic(viewControllerScope) {
        override fun notifyWalletStateLoaded() {
            runOnMainThread {
                walletTitle.text = texts.format(STRING_LABEL_SEND_FROM, wallet!!.walletConfig.displayName)
                // TODO hintReadonly.isHidden = if (uiLogic.wallet!!.walletConfig.secretStorage != null)
                scrollView.isHidden = false
            }
        }

        override fun notifyDerivedAddressChanged() {
            runOnMainThread {
                addressNameLabel.text = derivedAddress?.getAddressLabel(IosStringProvider(texts))
                    ?: texts.format(STRING_LABEL_ALL_ADDRESSES, wallet?.getNumOfAddresses())
            }
        }

        override fun notifyTokensChosenChanged() {
            // TODO
        }

        override fun notifyAmountsChanged() {
            runOnMainThread {
                // TODO tvFee.text = texts.format(STRING_DESC_FEE, feeAmount.toStringRoundToDecimals(4))
                // TODO grossAmount.texts = feeAmount.toStringRoundToDecimals(4)
                val nodeConnector = NodeConnector.getInstance()
                fiatLabel.isHidden = (nodeConnector.fiatCurrency.isEmpty())
                fiatLabel.text = texts.format(
                    STRING_LABEL_FIAT_AMOUNT,
                    formatFiatToString(
                        amountToSend.toDouble() * nodeConnector.fiatValue.value.toDouble(),
                        nodeConnector.fiatCurrency, IosStringProvider(texts)
                    )
                )
            }
        }

        override fun notifyBalanceChanged() {
            runOnMainThread {
                balanceLabel.text = texts.format(STRING_LABEL_WALLET_BALANCE, balance.toStringRoundToDecimals(4))
            }
        }

        override fun notifyUiLocked(locked: Boolean) {
            TODO("Not yet implemented")
        }

        override fun notifyHasTxId(txId: String) {
            TODO("Not yet implemented")
        }

        override fun notifyHasErgoTxResult(txResult: TransactionResult) {
            TODO("Not yet implemented")
        }

        override fun notifyHasSigningPromptData(signingPrompt: String?) {
            TODO("Not yet implemented")
        }
    }
}