package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.QR_DATA_LENGTH_LIMIT
import org.ergoplatform.transactions.QR_DATA_LENGTH_LOW_RES
import org.ergoplatform.uilogic.STRING_BUTTON_NEXT
import org.ergoplatform.uilogic.STRING_LABEL_QR_PAGES_INFO
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

abstract class PagedQrCodeContainer(
    protected val texts: I18NBundle,
    lastPageButtonTextLabel: String,
    private val descriptionLabel: String,
    private val lastPageDescriptionLabel: String,
) : UIView(CGRect.Zero()) {

    private var qrPages: List<String> = emptyList()
    private var showLowRes = false
    var rawData: String? = null
        set(value) {
            field = value
            switchResButton.isHidden = (value?.length ?: 0) <= QR_DATA_LENGTH_LOW_RES
            setQrCodesToPager()
        }

    private val qrContainer = UIStackView().apply {
        axis = UILayoutConstraintAxis.Horizontal
    }
    private val pager = qrContainer.wrapInHorizontalPager(DEFAULT_QR_CODE_SIZE + DEFAULT_MARGIN * 2).apply {
        delegate = object : UIScrollViewDelegateAdapter() {
            override fun didEndDecelerating(scrollView: UIScrollView?) {
                super.didEndDecelerating(scrollView)
                pageChanged(page)
            }
        }
    }
    private val description = Body1Label()
    private val currPageLabel = Headline2Label()
    private val nextButton = PrimaryButton(texts.get(STRING_BUTTON_NEXT)).apply {
        addOnTouchUpInsideListener { _, _ ->
            val nextPage = pager.page + 1
            scrollToQrCode(nextPage)
        }
    }
    private val continueButton = PrimaryButton(texts.get(lastPageButtonTextLabel)).apply {
        addOnTouchUpInsideListener { _, _ ->
            continueButtonPressed()
        }
    }

    private val descContainer = UIView(CGRect.Zero()).apply {
        layoutMargins = UIEdgeInsets.Zero()
    }

    private val switchResButton =
        UIImageView(getIosSystemImage(IMAGE_SWITCH_RESOLUTION, UIImageSymbolScale.Small)).apply {
            tintColor = UIColor.label()
            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                showLowRes = !showLowRes
                setQrCodesToPager()
            })
        }

    init {
        this.addSubview(pager)
        this.addSubview(descContainer)
        this.addSubview(switchResButton)
        this.addSubview(currPageLabel)

        switchResButton.topToSuperview().rightToSuperview()
        pager.topToBottomOf(switchResButton, DEFAULT_MARGIN).centerHorizontal()
        descContainer.topToBottomOf(currPageLabel, DEFAULT_MARGIN * 3).widthMatchesSuperview(maxWidth = MAX_WIDTH)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)
        currPageLabel.topToBottomOf(pager, DEFAULT_MARGIN).centerHorizontal()

        descContainer.apply {
            addSubview(description)
            addSubview(nextButton)
            addSubview(continueButton)

            description.topToSuperview().widthMatchesSuperview()
            nextButton.topToBottomOf(description, DEFAULT_MARGIN * 3).centerHorizontal().fixedWidth(120.0)
            continueButton.topToTopOf(nextButton).centerHorizontal().fixedWidth(200.0)
                .bottomToSuperview(canBeLess = true)
        }

    }

    private fun setQrCodesToPager() {
        rawData?.let { rawData ->
            qrPages = calcChunksFromRawData(rawData, if (showLowRes) QR_DATA_LENGTH_LOW_RES else QR_DATA_LENGTH_LIMIT)
            qrContainer.clearArrangedSubviews()
            qrPages.forEach {
                val qrCode = UIImageView(CGRect.Zero())
                qrCode.fixedWidth(DEFAULT_QR_CODE_SIZE + DEFAULT_MARGIN * 2).fixedHeight(DEFAULT_QR_CODE_SIZE)
                qrCode.setQrCode(it, DEFAULT_QR_CODE_SIZE)
                qrCode.contentMode = UIViewContentMode.Center
                qrContainer.addArrangedSubview(qrCode)
            }
            pager.layoutIfNeeded()
            scrollToQrCode(0)
        }
    }

    abstract fun calcChunksFromRawData(rawData: String, limit: Int): List<String>

    private fun scrollToQrCode(nextPage: Int) {
        pager.page = nextPage
        pageChanged(nextPage)
    }

    private fun pageChanged(newPage: Int) {
        val texts = getAppDelegate().texts
        val lastPage = newPage == qrPages.size - 1
        currPageLabel.text =
            if (qrPages.size == 1) ""
            else texts.format(STRING_LABEL_QR_PAGES_INFO, newPage + 1, qrPages.size)

        descContainer.animateLayoutChanges {
            description.text = texts.get(
                if (lastPage) lastPageDescriptionLabel
                else descriptionLabel
            )
            nextButton.isHidden = lastPage
            continueButton.isHidden = !lastPage
        }
    }

    abstract fun continueButtonPressed()
}