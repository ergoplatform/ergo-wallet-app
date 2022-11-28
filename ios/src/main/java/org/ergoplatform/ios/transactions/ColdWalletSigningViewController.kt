package org.ergoplatform.ios.transactions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.QrCodePagesCollector
import org.ergoplatform.transactions.SigningResult
import org.ergoplatform.transactions.coldSigningResponseToQrChunks
import org.ergoplatform.transactions.reduceBoxes
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.ColdWalletSigningUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class ColdWalletSigningViewController(
    private val signingRequestChunk: String,
    private val walletId: Int
) : CoroutineViewController() {

    val uiLogic = IosUiLogic()
    val texts = getAppDelegate().texts

    private val scanningContainer = ScanningContainer(::scanNext)
    private val transactionContainer = SigningTransactionContainer(texts, this) {
        startAuthFlow(uiLogic.wallet!!.walletConfig) { mnemonic ->
            uiLogic.signTxWithMnemonicAsync(mnemonic, IosStringProvider(texts))
        }
    }
    private val signedQrCodesContainer = SignedQrCodeContainer()

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
        scrollingContainer.addSubview(scanningContainer)
        scrollingContainer.addSubview(transactionContainer)
        scrollingContainer.addSubview(signedQrCodesContainer)

        scanningContainer.edgesToSuperview(maxWidth = MAX_WIDTH)
        transactionContainer.edgesToSuperview(maxWidth = MAX_WIDTH)
        signedQrCodesContainer.edgesToSuperview(maxWidth = MAX_WIDTH)
        scrollView.edgesToSuperview()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        uiLogic.setWalletId(walletId, getAppDelegate().database.walletDbProvider)
        addQrCodeChunk(signingRequestChunk)
    }

    private fun addQrCodeChunk(qrCodeChunk: String) {
        uiLogic.addQrCodeChunk(qrCodeChunk, IosStringProvider(texts))

        scanningContainer.refreshTexts(uiLogic.qrPagesCollector, uiLogic.lastErrorMessage)

        uiLogic.transactionInfo?.let {
            transactionContainer.bindTransaction(it, null,
                addressLabelHandler = { address, callback ->
                    viewControllerScope.launch {
                        val appDelegate = getAppDelegate()
                        getAddressLabelFromDatabase(
                            appDelegate.database, address,
                            IosStringProvider(appDelegate.texts)
                        )?.let { runOnMainThread { callback(it) } }
                    }
                })
        }

        refreshUiState()
    }

    private fun refreshUiState() {
        scanningContainer.isHidden = uiLogic.state != ColdWalletSigningUiLogic.State.SCANNING
        transactionContainer.isHidden =
            uiLogic.state != ColdWalletSigningUiLogic.State.WAITING_TO_CONFIRM
        signedQrCodesContainer.isHidden =
            uiLogic.state != ColdWalletSigningUiLogic.State.PRESENT_RESULT
    }

    private fun scanNext() {
        presentViewController(QrScannerViewController(invokeAfterDismissal = false) {
            addQrCodeChunk(it)
        }, true) {}
    }

    class ScanningContainer(
        onScanNext: () -> Unit
    ) : UIView(CGRect.Zero()) {
        val statusText = Headline2Label().apply {
            textAlignment = NSTextAlignment.Center
        }
        val errorText = Body1BoldLabel().apply {
            textColor = uiColorErgo
            textAlignment = NSTextAlignment.Center
        }

        init {
            val image = UIImageView(
                getIosSystemImage(IMAGE_QR_CODE, UIImageSymbolScale.Large, 150.0)
            ).apply {
                contentMode = UIViewContentMode.Center
                tintColor = UIColor.secondaryLabel()
            }
            val scanNextButton = PrimaryButton(
                getAppDelegate().texts.get(STRING_LABEL_SCAN_QR),
                getIosSystemImage(IMAGE_QR_SCAN, UIImageSymbolScale.Small)
            ).apply {
                addOnTouchUpInsideListener { _, _ -> onScanNext() }
            }

            addSubview(image)
            addSubview(statusText)
            addSubview(errorText)
            addSubview(scanNextButton)

            image.fixedHeight(200.0).topToSuperview().widthMatchesSuperview()
            statusText.topToBottomOf(image, DEFAULT_MARGIN * 2).widthMatchesSuperview()
            errorText.topToBottomOf(statusText, DEFAULT_MARGIN * 2).widthMatchesSuperview()
            scanNextButton.topToBottomOf(errorText, DEFAULT_MARGIN * 2)
                .bottomToSuperview(canBeLess = true)
                .fixedWidth(200.0).centerHorizontal()
        }

        fun refreshTexts(qrPagesCollector: QrCodePagesCollector, errorMessage: String?) {
            statusText.text =
                getAppDelegate().texts.format(
                    STRING_LABEL_QR_PAGES_INFO,
                    qrPagesCollector.pagesAdded,
                    qrPagesCollector.pagesCount
                )
            errorText.text = errorMessage ?: ""
        }
    }

    inner class SignedQrCodeContainer : PagedQrCodeContainer(
        texts,
        STRING_LABEL_DISMISS,
        descriptionLabel = STRING_DESC_SHOW_SIGNED_MULTIPLE,
        lastPageDescriptionLabel = STRING_DESC_SHOW_SIGNED,
    ) {
        override fun calcChunksFromRawData(rawData: String, limit: Int): List<String> {
            return coldSigningResponseToQrChunks(rawData, limit)
        }

        override fun continueButtonPressed() {
            navigationController.popViewController(true)
        }
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
            runOnMainThread {
                if (ergoTxResult.success && uiLogic.signedQrCode != null) {
                    signedQrCodesContainer.rawData = uiLogic.signedQrCode
                    refreshUiState()
                } else {
                    val message = texts.get(STRING_ERROR_PREPARE_TRANSACTION) +
                            ergoTxResult.errorMsg?.let { "\n\n$it" }
                    presentViewController(buildSimpleAlertController("", message, texts), true) {}
                }
            }
        }
    }
}