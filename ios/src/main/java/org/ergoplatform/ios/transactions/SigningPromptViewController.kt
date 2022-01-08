package org.ergoplatform.ios.transactions

import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.getColdSignedTxChunk
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.transactions.SendFundsUiLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class SigningPromptViewController(
    private val qrPages: List<String>,
    private val uiLogic: SendFundsUiLogic
) : UIViewController() {
    private lateinit var description: UILabel
    private lateinit var pager: UIScrollView
    private lateinit var nextButton: PrimaryButton
    private lateinit var scanButton: PrimaryButton
    private lateinit var descContainer: UIView

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()
        val closeButton = addCloseButton()

        val qrContainer = UIStackView(CGRect.Zero()).apply {
            axis = UILayoutConstraintAxis.Horizontal
        }

        qrPages.forEach {
            val qrCode = UIImageView(CGRect.Zero())
            qrCode.fixedWidth(DEFAULT_QR_CODE_SIZE + DEFAULT_MARGIN * 2).fixedHeight(DEFAULT_QR_CODE_SIZE)
            qrCode.setQrCode(it, DEFAULT_QR_CODE_SIZE)
            qrCode.contentMode = UIViewContentMode.Center
            qrContainer.addArrangedSubview(qrCode)
        }

        pager = qrContainer.wrapInHorizontalPager(DEFAULT_QR_CODE_SIZE + DEFAULT_MARGIN * 2)
        pager.delegate = object : UIScrollViewDelegateAdapter() {
            override fun didEndDecelerating(scrollView: UIScrollView?) {
                super.didEndDecelerating(scrollView)
                pageChanged()
            }

            override fun didEndScrollingAnimation(scrollView: UIScrollView?) {
                super.didEndScrollingAnimation(scrollView)
                pageChanged()
            }
        }

        description = Body1Label()

        val texts = getAppDelegate().texts
        nextButton = PrimaryButton(texts.get(STRING_BUTTON_NEXT))
        nextButton.addOnTouchUpInsideListener { _, _ ->
            pager.page = pager.page + 1
        }

        scanButton = PrimaryButton(texts.get(STRING_BUTTON_SCAN_SIGNED_TX))
        scanButton.addOnTouchUpInsideListener { _, _ ->
            presentViewController(QrScannerViewController { qrCode ->
                getColdSignedTxChunk(qrCode)?.let {
                    if (it.pages > 1) {
                        // TODO cold wallet handle paged QR codes
                        val uac =
                            buildSimpleAlertController("", texts.get(STRING_ERROR_QR_PAGES_NUM), getAppDelegate().texts)
                        presentViewController(uac, false) {}
                    } else {
                        dismissViewController(false) {
                            val delegate = getAppDelegate()
                            uiLogic.sendColdWalletSignedTx(
                                listOf(qrCode),
                                delegate.prefs,
                                IosStringProvider(delegate.texts)
                            )
                        }
                    }
                }
            }, true) {}
        }

        descContainer = UIView(CGRect.Zero()).apply {
            layoutMargins = UIEdgeInsets.Zero()
        }

        descContainer.addSubview(description)
        descContainer.addSubview(nextButton)
        descContainer.addSubview(scanButton)
        view.addSubview(pager)
        view.addSubview(descContainer)

        pager.topToBottomOf(closeButton, DEFAULT_MARGIN).centerHorizontal()
        descContainer.topToBottomOf(pager, DEFAULT_MARGIN * 3).widthMatchesSuperview(maxWidth = MAX_WIDTH)
        description.topToSuperview().widthMatchesSuperview(inset = DEFAULT_MARGIN)
        nextButton.topToBottomOf(description, DEFAULT_MARGIN * 3).centerHorizontal().fixedWidth(120.0)
        scanButton.topToTopOf(nextButton).centerHorizontal().fixedWidth(200.0).bottomToSuperview()

        pageChanged()
    }

    private fun pageChanged() {
        val texts = getAppDelegate().texts
        val lastPage = pager.page == qrPages.size - 1

        descContainer.animateLayoutChanges {
            description.text = texts.get(
                if (lastPage) STRING_DESC_PROMPT_SIGNING
                else STRING_DESC_PROMPT_SIGNING_MULTIPLE
            )
            nextButton.isHidden = lastPage
            scanButton.isHidden = !lastPage
        }
    }
}