package org.ergoplatform.ios.tokens

import org.ergoplatform.getExplorerTokenUrl
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.ios.ui.*
import org.ergoplatform.tokens.getHttpContentLink
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.tokens.TokenInformationLayoutLogic
import org.ergoplatform.uilogic.tokens.TokenInformationModelLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.foundation.NSData
import org.robovm.apple.uikit.*

class TokenInformationLayoutView(private val vc: TokenInformationViewController) : UIView(CGRect.Zero()) {
    private val layoutLogic = IosTokenInformationLayoutLogic()
    private val modelLogic = vc.uiLogic
    private val texts = getAppDelegate().texts
    private val nameLabel = Headline2Label().apply {
        numberOfLines = 1
        textAlignment = NSTextAlignment.Center
    }
    private val genuineImageContainer = GenuineImageContainer()
    private val tokenIdLabel = Body2Label().apply {
        numberOfLines = 1
        textAlignment = NSTextAlignment.Center
        lineBreakMode = NSLineBreakMode.TruncatingMiddle
        textColor = UIColor.secondaryLabel()
    }
    private val descriptionLabel = Body1Label().apply {
        numberOfLines = 5
        textAlignment = NSTextAlignment.Center
    }
    private val mintingTxLabel = Body1BoldLabel().apply {
        text = texts.get(STRING_LABEL_MINTING_TX)
    }
    private val supplyAmountLabel = Body1Label().apply {
        textAlignment = NSTextAlignment.Center
    }
    private val supplyAmountTitle = Body1BoldLabel().apply {
        text = texts.get(STRING_LABEL_TOKEN_SUPPLY)
        textAlignment = NSTextAlignment.Center
    }
    private val balanceAmountLabel = Body1Label().apply {
        textAlignment = NSTextAlignment.Center
    }
    private val balanceAmountValue = Body1Label().apply {
        textAlignment = NSTextAlignment.Center
        textColor = UIColor.secondaryLabel()
    }
    private val balanceAmountTitle = Body1BoldLabel().apply {
        textAlignment = NSTextAlignment.Center
        text = texts.get(STRING_TITLE_WALLET_BALANCE)
    }
    private val nftLayout = NftLayoutView()

    init {

        val topStack = UIStackView(NSArray(nameLabel, genuineImageContainer)).apply {
            axis = UILayoutConstraintAxis.Horizontal
        }
        val openBrowserImage = getIosSystemImage(IMAGE_OPEN_BROWSER, UIImageSymbolScale.Small, 20.0)!!
        val tokenIdView = tokenIdLabel.wrapWithTrailingImage(openBrowserImage, keepWidth = true)
        val descriptionTitleLabel = Body1BoldLabel().apply {
            text = texts.get(STRING_LABEL_TOKEN_DESCRIPTION)
            textAlignment = NSTextAlignment.Center
        }
        val mintingTxTitleView = mintingTxLabel.wrapWithTrailingImage(openBrowserImage, keepWidth = true)

        val optionalEntriesStack = UIStackView(
            NSArray(
                nftLayout,
                balanceAmountTitle,
                balanceAmountLabel,
                balanceAmountValue,
                supplyAmountTitle,
                supplyAmountLabel
            )
        ).apply {
            axis = UILayoutConstraintAxis.Vertical
            spacing = DEFAULT_MARGIN * 3
            setCustomSpacing(DEFAULT_MARGIN / 2, balanceAmountTitle)
            setCustomSpacing(DEFAULT_MARGIN / 2, supplyAmountTitle)
        }

        addSubview(topStack)
        addSubview(tokenIdView)
        addSubview(optionalEntriesStack)
        addSubview(descriptionTitleLabel)
        addSubview(descriptionLabel)
        addSubview(mintingTxTitleView)

        topStack.topToSuperview(topInset = DEFAULT_MARGIN).centerHorizontal(true)
        tokenIdView.topToBottomOf(topStack, DEFAULT_MARGIN / 2).centerHorizontal(true)
        optionalEntriesStack.topToBottomOf(tokenIdView, DEFAULT_MARGIN * 3).widthMatchesSuperview()
        descriptionTitleLabel.topToBottomOf(optionalEntriesStack, DEFAULT_MARGIN * 3).widthMatchesSuperview()
        descriptionLabel.topToBottomOf(descriptionTitleLabel).widthMatchesSuperview()
        mintingTxTitleView.topToBottomOf(descriptionLabel, DEFAULT_MARGIN * 3).centerHorizontal(true)

        mintingTxTitleView.bottomToSuperview(canBeLess = true)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        tokenIdLabel.apply {
            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                openUrlInBrowser(getExplorerTokenUrl(tokenIdLabel.text))
            })
        }
        descriptionLabel.apply {
            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                animateLayoutChanges {
                    numberOfLines = if (numberOfLines == 5L) 100 else 5
                }
            })
        }
        mintingTxLabel.apply {
            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                modelLogic.tokenInformation?.let { openUrlInBrowser(getExplorerTxUrl(it.mintingTxId)) }
            })
        }
        nftLayout.contentLinkLabel.apply {
            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                modelLogic.eip4Token?.getHttpContentLink(getAppDelegate().prefs)?.let {
                    openUrlInBrowser(it)
                }
            })
        }
        nftLayout.contentHashLabel.apply {
            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                vc.shareText(text, this)
            })
        }
        nftLayout.activateDownloadButton.addOnTouchUpInsideListener { _, _ ->
            modelLogic.downloadContent(getAppDelegate().prefs)
        }
    }

    fun updateTokenInformation(balanceAmount: Long?) {
        layoutLogic.updateLayout(modelLogic, IosStringProvider(texts), balanceAmount ?: 0)
    }

    fun updateNftPreview() {
        layoutLogic.updateNftPreview(modelLogic)
    }

    inner class NftLayoutView : UIStackView() {
        val thumbnailContainer = ThumbnailContainer(90.0)
        val contentLinkLabel = Body1Label().apply {
            textAlignment = NSTextAlignment.Center
            numberOfLines = 3
            lineBreakMode = NSLineBreakMode.TruncatingMiddle
            lineBreakStrategy = NSLineBreakStrategy.None
        }
        val contentHashLabel = Body1Label().apply {
            textAlignment = NSTextAlignment.Center
            numberOfLines = 3
            lineBreakMode = NSLineBreakMode.TruncatingMiddle
        }
        val contentHashContainer = contentHashLabel.wrapWithTrailingImage(
            getHashValidImage(false), 30.0, 30.0
        )
        val activateDownloadButton = TextButton(texts.get(STRING_BUTTON_DOWNLOAD_CONTENT_ON))
        val activateDownloadContainer = UIView(CGRect.Zero()).apply {
            layer.borderWidth = 1.0
            layer.cornerRadius = 4.0
            layer.borderColor = uiColorErgo.cgColor
            isHidden = true

            val labelDesc = Body1Label().apply {
                textAlignment = NSTextAlignment.Center
                text = texts.get(STRING_DESC_DOWNLOAD_CONTENT)
            }

            addSubview(labelDesc)
            addSubview(activateDownloadButton)
            labelDesc.topToSuperview(topInset = DEFAULT_MARGIN).widthMatchesSuperview(inset = DEFAULT_MARGIN)
            activateDownloadButton.topToBottomOf(labelDesc, DEFAULT_MARGIN * 2).centerHorizontal()
                .bottomToSuperview(bottomInset = DEFAULT_MARGIN)
        }
        val previewContainer = NftPreviewContainer()

        fun getHashValidImage(verified: Boolean) =
            getIosSystemImage(
                if (verified) IMAGE_VERIFIED
                else IMAGE_SUSPICIOUS, UIImageSymbolScale.Small
            )!!

        init {
            axis = UILayoutConstraintAxis.Vertical
            spacing = DEFAULT_MARGIN * 3

            val contentLinkTitle = Body1BoldLabel().apply {
                textAlignment = NSTextAlignment.Center
                text = texts.get(STRING_LABEL_CONTENT_LINK)
            }
            val contentHashTitle = Body1BoldLabel().apply {
                textAlignment = NSTextAlignment.Center
                text = texts.get(STRING_LABEL_CONTENT_HASH)
            }
            contentHashContainer.trailingImage.apply {
                isHidden = true
                tintColor = uiColorErgo
            }

            addArrangedSubview(previewContainer)
            addArrangedSubview(thumbnailContainer)
            addArrangedSubview(activateDownloadContainer)
            addArrangedSubview(contentLinkTitle)
            addArrangedSubview(contentLinkLabel)
            setCustomSpacing(DEFAULT_MARGIN / 2, contentLinkTitle)
            addArrangedSubview(contentHashTitle)
            addArrangedSubview(contentHashContainer)
            setCustomSpacing(DEFAULT_MARGIN / 2, contentHashTitle)
        }
    }

    class NftPreviewContainer : UIView(CGRect.Zero()) {
        private val progressView = UIActivityIndicatorView(UIActivityIndicatorViewStyle.Large)
        private val previewImg = UIImageView(CGRect.Zero())
        private val previewImageHeight: NSLayoutConstraint
        private var imageLoaded = false

        init {
            layoutMargins = UIEdgeInsets.Zero()
            isHidden = true
            addSubview(previewImg)
            addSubview(progressView)

            progressView.centerHorizontal().centerVertical()
            previewImg.apply {
                leftToSuperview().rightToSuperview().centerInSuperviewWhenSmaller()
                contentMode = UIViewContentMode.ScaleAspectFit
                setTranslatesAutoresizingMaskIntoConstraints(false)
                previewImageHeight = heightAnchor.equalTo(100.0)
                NSLayoutConstraint.activateConstraints(NSArray(previewImageHeight))
            }

            minHeight(100.0)

        }

        fun showProgress(showProgress: Boolean) {
            if (showProgress) {
                progressView.isHidden = false
                progressView.startAnimating()
            } else {
                progressView.isHidden = true
                progressView.stopAnimating()
            }
        }

        fun showImage(content: ByteArray?, downloadError: Boolean) {
            if (imageLoaded)
                return

            val hasError = content?.let {
                try {
                    imageLoaded = true
                    val image = UIImage(NSData(it))
                    val sizeRatio = image.size.width / image.size.height
                    val frameWidth = previewImg.frame.width
                    val frameHeight = frameWidth / sizeRatio
                    previewImageHeight.constant = frameHeight

                    previewImg.image = image.scaleToSize(frameWidth, frameHeight)
                    previewImg.tintColor = null

                    false
                } catch (t: Throwable) {
                    true
                }
            } ?: false

            if (hasError || downloadError) {
                previewImageHeight.constant = 100.0
                previewImg.tintColor = UIColor.label()
                previewImg.image = getIosSystemImage(IMAGE_WARNING, UIImageSymbolScale.Large)
            }
        }
    }

    inner class IosTokenInformationLayoutLogic : TokenInformationLayoutLogic() {
        override fun setTokenTextFields(displayName: String, tokenId: String, description: String) {
            nameLabel.text = displayName
            tokenIdLabel.text = tokenId
            descriptionLabel.text = description
        }

        override fun setLabelSupplyAmountText(supplyAmount: String?) {
            supplyAmount?.let {
                this@TokenInformationLayoutView.supplyAmountLabel.text = supplyAmount
            }
            this@TokenInformationLayoutView.supplyAmountLabel.isHidden = supplyAmount == null
            supplyAmountTitle.isHidden = supplyAmount == null
        }

        override fun setTokenGenuine(genuineFlag: Int) {
            genuineImageContainer.setGenuineFlag(genuineFlag)
        }

        override fun setBalanceAmountAndValue(amount: String?, balanceValue: String?) {
            balanceAmountLabel.text = amount ?: ""
            balanceAmountValue.text = balanceValue ?: ""
            balanceAmountValue.isHidden = balanceValue == null
            balanceAmountLabel.isHidden = amount == null
            balanceAmountTitle.isHidden = amount == null

            val stackView = balanceAmountLabel.superview as? UIStackView
            stackView?.setCustomSpacing(
                if (balanceAmountValue.isHidden) stackView.spacing else DEFAULT_MARGIN / 2,
                balanceAmountLabel
            )
        }

        override fun setContentLinkText(linkText: String) {
            nftLayout.contentLinkLabel.text = linkText
        }

        override fun setNftLayoutVisibility(visible: Boolean) {
            nftLayout.isHidden = !visible
        }

        override fun setContentHashText(hashText: String) {
            nftLayout.contentHashLabel.text = hashText
        }

        override fun setThumbnail(thumbnailType: Int) {
            nftLayout.thumbnailContainer.setThumbnail(thumbnailType)
        }

        override fun showNftPreview(
            downloadState: TokenInformationModelLogic.StateDownload,
            downloadPercent: Float,
            content: ByteArray?
        ) {
            nftLayout.activateDownloadContainer.isHidden =
                downloadState != TokenInformationModelLogic.StateDownload.NOT_STARTED

            nftLayout.previewContainer.isHidden = when (downloadState) {
                TokenInformationModelLogic.StateDownload.NOT_AVAILABLE -> true
                TokenInformationModelLogic.StateDownload.NOT_STARTED -> true
                TokenInformationModelLogic.StateDownload.RUNNING -> false
                TokenInformationModelLogic.StateDownload.DONE -> false
                TokenInformationModelLogic.StateDownload.ERROR -> false
            }

            nftLayout.previewContainer.showProgress(
                downloadState == TokenInformationModelLogic.StateDownload.RUNNING
            )

            nftLayout.previewContainer.showImage(
                content,
                downloadState == TokenInformationModelLogic.StateDownload.ERROR
            )
        }

        override fun showNftHashValidation(hashValid: Boolean?) {
            nftLayout.contentHashContainer.trailingImage.apply {
                isHidden = hashValid == null
                hashValid?.let {
                    image = nftLayout.getHashValidImage(it)
                }
            }
        }

    }
}