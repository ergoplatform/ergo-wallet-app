package org.ergoplatform.ios.tokens

import org.ergoplatform.getExplorerTokenUrl
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.uilogic.STRING_LABEL_MINTING_TX
import org.ergoplatform.uilogic.STRING_LABEL_TOKEN_DESCRIPTION
import org.ergoplatform.uilogic.STRING_LABEL_TOKEN_SUPPLY
import org.ergoplatform.uilogic.STRING_TITLE_WALLET_BALANCE
import org.ergoplatform.uilogic.tokens.TokenInformationLayoutLogic
import org.ergoplatform.uilogic.tokens.TokenInformationModelLogic
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.uikit.*

class TokenInformationLayoutView : UIView(CGRect.Zero()) {
    private val layoutLogic = IosTokenInformationLayoutLogic()
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

    private var tokenInformation: TokenInformation? = null

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
                balanceAmountTitle, balanceAmountLabel, balanceAmountValue, supplyAmountTitle, supplyAmountLabel
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
                tokenInformation?.let { openUrlInBrowser(getExplorerTxUrl(it.mintingTxId)) }
            })
        }
    }

    fun updateTokenInformation(modelLogic: TokenInformationModelLogic, balanceAmount: Long?) {
        tokenInformation = modelLogic.tokenInformation
        layoutLogic.updateLayout(modelLogic, IosStringProvider(texts), balanceAmount ?: 0)
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

            val stackview = balanceAmountLabel.superview as? UIStackView
            stackview?.setCustomSpacing(
                if (balanceAmountValue.isHidden) stackview.spacing else DEFAULT_MARGIN / 2,
                balanceAmountLabel
            )
        }

        override fun setContentLinkText(linkText: String) {
            // todo token
        }

        override fun setNftLayoutVisibility(visible: Boolean) {
            // TODO("Not yet implemented")
        }

        override fun setContentHashText(hashText: String) {
            // TODO("Not yet implemented")
        }

        override fun setThumbnail(thumbnailType: Int) {
            // TODO("Not yet implemented")
        }

        override fun showNftPreview(
            downloadState: TokenInformationModelLogic.StateDownload,
            downloadPercent: Float,
            content: ByteArray?
        ) {
            // TODO("Not yet implemented")
        }

        override fun showNftHashValidation(hashValid: Boolean?) {
            // TODO("Not yet implemented")
        }

    }
}