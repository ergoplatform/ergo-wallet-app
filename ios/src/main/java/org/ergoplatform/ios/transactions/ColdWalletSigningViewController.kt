package org.ergoplatform.ios.transactions

import kotlinx.coroutines.CoroutineScope
import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.uilogic.STRING_LABEL_QR_PAGES_INFO
import org.ergoplatform.uilogic.STRING_LABEL_SCAN_QR
import org.ergoplatform.uilogic.STRING_TITLE_SIGNING_REQUEST
import org.ergoplatform.uilogic.transactions.ColdWalletSigningUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class ColdWalletSigningViewController(private val signingRequestChunk: String, private val walletId: Int) :
    CoroutineViewController() {

    val uiLogic = IosUiLogic()
    val texts = getAppDelegate().texts

    val scanningContainer = ScanningContainer()

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.layoutMargins = UIEdgeInsets.Zero()
        val texts = getAppDelegate().texts
        title = texts.get(STRING_TITLE_SIGNING_REQUEST)
        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        val scrollingContainer = UIView(CGRect.Zero())
        val scrollView = scrollingContainer.wrapInVerticalScrollView()

        view.addSubview(scrollView)
        scrollView.addSubview(scanningContainer)

        scanningContainer.edgesToSuperview()
        scrollView.edgesToSuperview()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        uiLogic.setWalletId(walletId, getAppDelegate().database)
        addQrCodeChunk(signingRequestChunk)
    }

    private fun addQrCodeChunk(qrCodeChunk: String) {
        uiLogic.addQrCodeChunk(qrCodeChunk)

        scanningContainer.statusText.text =
            texts.format(STRING_LABEL_QR_PAGES_INFO, uiLogic.pagesAdded, uiLogic.pagesQrCode)
        scanningContainer.errorText.text = uiLogic.lastErrorMessage ?: ""

        uiLogic.transactionInfo?.let {
            // TODO new tx info, fill container
        }

        refreshState()
    }

    private fun refreshState() {
        scanningContainer.isHidden = uiLogic.state != ColdWalletSigningUiLogic.State.SCANNING
    }

    inner class ScanningContainer : UIView(CGRect.Zero()) {
        val statusText = Headline2Label().apply {
            textAlignment = NSTextAlignment.Center
        }
        val errorText = Body1BoldLabel().apply {
            textColor = uiColorErgo
            textAlignment = NSTextAlignment.Center
        }

        init {
            val image = UIImageView(getIosSystemImage(IMAGE_QR_CODE, UIImageSymbolScale.Large, 150.0)).apply {
                contentMode = UIViewContentMode.Center
                tintColor = UIColor.secondaryLabel()
            }
            val scanNextButton = PrimaryButton(
                texts.get(STRING_LABEL_SCAN_QR),
                getIosSystemImage(IMAGE_QR_SCAN, UIImageSymbolScale.Small)
            ).apply {
                addOnTouchUpInsideListener { _, _ -> scanNext() }
            }

            addSubview(image)
            addSubview(statusText)
            addSubview(errorText)
            addSubview(scanNextButton)

            image.fixedHeight(200.0).topToSuperview().widthMatchesSuperview()
            statusText.topToBottomOf(image, DEFAULT_MARGIN * 2).widthMatchesSuperview()
            errorText.topToBottomOf(statusText, DEFAULT_MARGIN * 2).widthMatchesSuperview()
            scanNextButton.topToBottomOf(errorText).bottomToSuperview(canBeLess = true).fixedWidth(200.0)
                .centerHorizontal()
        }
    }

    private fun scanNext() {
        presentViewController(QrScannerViewController {
            addQrCodeChunk(it)
        }, true) {}
    }

    inner class IosUiLogic : ColdWalletSigningUiLogic() {
        private val progressViewController =
            ProgressViewController.ProgressViewControllerPresenter(this@ColdWalletSigningViewController)

        override val coroutineScope: CoroutineScope
            get() = viewControllerScope

        override fun notifyUiLocked(locked: Boolean) {
            runOnMainThread {
                progressViewController.setUiLocked(locked)
            }
        }

        override fun notifySigningResult(ergoTxResult: SigningResult) {
            TODO("Not yet implemented")
        }
    }
}