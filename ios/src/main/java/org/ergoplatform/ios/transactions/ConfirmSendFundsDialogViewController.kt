package org.ergoplatform.ios.transactions

import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.TransactionInfo
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.UIColor
import org.robovm.apple.uikit.UIView

class ConfirmSendFundsDialogViewController(
    private val transactionInfo: TransactionInfo,
    private val onConfirm: () -> Unit,
) : CoroutineViewController() {

    private lateinit var tic: SigningTransactionContainer

    override fun viewDidLoad() {
        super.viewDidLoad()

        tic = SigningTransactionContainer(getAppDelegate().texts, this, onConfirm = {
            dismissViewController(true) { onConfirm() }
        })

        view.backgroundColor = UIColor.systemBackground()
        val closeButton = addCloseButton()

        val content = UIView(CGRect.Zero())
        content.addSubview(tic)
        tic.edgesToSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
        val scrollView = content.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.topToBottomOf(closeButton).bottomToSuperview(true).widthMatchesSuperview(true)

    }

    private var hasBound = false

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        if (!hasBound) {
            hasBound = true
            tic.bindTransaction(
                transactionInfo, tokenClickListener = null,
                tic.defaultAddressLabelHandler(viewControllerScope),
                tic.defaultTokenLabelHandler(viewControllerScope)
            )
        }
    }

}