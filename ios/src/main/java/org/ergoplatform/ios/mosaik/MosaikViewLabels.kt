package org.ergoplatform.ios.mosaik

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.TokenAmount
import org.ergoplatform.ios.tokens.GenuineImageContainer
import org.ergoplatform.ios.tokens.ThumbnailContainer
import org.ergoplatform.ios.ui.*
import org.ergoplatform.mosaik.LabelFormatter
import org.ergoplatform.mosaik.TreeElement
import org.ergoplatform.mosaik.model.ui.ForegroundColor
import org.ergoplatform.mosaik.model.ui.MarkDown
import org.ergoplatform.mosaik.model.ui.layout.HAlignment
import org.ergoplatform.mosaik.model.ui.text.*
import org.ergoplatform.persistance.GENUINE_UNKNOWN
import org.ergoplatform.persistance.THUMBNAIL_TYPE_NONE
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.tokens.TokenInfoManager
import org.ergoplatform.uilogic.STRING_LABEL_UNNAMED_TOKEN
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class LabelViewHolder(treeElement: TreeElement, mosaikViewElement: StyleableTextLabel<*>) : UiViewHolder(
    mosaikViewElement.style.toUiLabelView(), treeElement
) {
    private val labelView = uiView as ThemedLabel
    private val isExpandable = (mosaikViewElement is ExpandableElement && mosaikViewElement.isExpandOnClick)
    private var isExpanded = false

    init {
        labelView.apply {
            text = LabelFormatter.getFormattedText(mosaikViewElement, treeElement)
            textColor = mosaikViewElement.textColor.toTextUiColor()
            setTextLabelProperties(mosaikViewElement)

            if (isExpandable) {
                isUserInteractionEnabled = true
                addGestureRecognizer(UILongPressGestureRecognizer {
                    mosaikViewController.lastViewInteracted = this
                    treeElement.longPressed()
                })
                addGestureRecognizer(UITapGestureRecognizer {
                    mosaikViewController.lastViewInteracted = this
                    isExpanded = !isExpanded
                    setMaxLines()
                })
                setMaxLines()
            }
        }
    }

    private fun setMaxLines() {
        labelView.numberOfLines = if (isExpandable && !isExpanded) 1
        else (treeElement.element as StyleableTextLabel<*>).maxLines.toLong()
    }

    override val handlesClicks: Boolean
        get() = isExpandable || super.handlesClicks
}

private fun ForegroundColor.toTextUiColor() =
    when (this) {
        ForegroundColor.PRIMARY -> uiColorErgo
        ForegroundColor.DEFAULT -> UIColor.label()
        ForegroundColor.SECONDARY -> UIColor.secondaryLabel()
    }

private fun LabelStyle.toUiLabelView() =
    when (this) {
        LabelStyle.BODY1 -> Body1Label()
        LabelStyle.BODY1BOLD -> Body1BoldLabel()
        LabelStyle.BODY1LINK -> Body1Label() // TODO
        LabelStyle.BODY2 -> Body2Label()
        LabelStyle.BODY2BOLD -> Body2BoldLabel()
        LabelStyle.HEADLINE1 -> Headline1Label()
        LabelStyle.HEADLINE2 -> Headline2Label()
    }

private fun UILabel.setTextLabelProperties(mosaikViewElement: TextLabel<*>) {
    numberOfLines = mosaikViewElement.maxLines.toLong()
    if (mosaikViewElement.maxLines == 1) lineBreakStrategy = NSLineBreakStrategy.None
    lineBreakMode = if (mosaikViewElement.maxLines > 0)
        mosaikViewElement.truncationType.toUiViewLineBreakMode()
    else
        NSLineBreakMode.WordWrapping

    textAlignment = mosaikViewElement.textAlignment.toTextAlignment()
}

private fun HAlignment.toTextAlignment() =
    when (this) {
        HAlignment.START -> NSTextAlignment.Left
        HAlignment.CENTER -> NSTextAlignment.Center
        HAlignment.END -> NSTextAlignment.Right
        HAlignment.JUSTIFY -> NSTextAlignment.Justified
    }

private fun TruncationType.toUiViewLineBreakMode() =
    when (this) {
        TruncationType.START -> NSLineBreakMode.TruncatingHead
        TruncationType.MIDDLE -> NSLineBreakMode.TruncatingMiddle
        TruncationType.END -> NSLineBreakMode.TruncatingTail
    }

class ButtonHolder(treeElement: TreeElement) : UiViewHolder(
    UIView(CGRect.Zero()),
    treeElement
) {
    init {
        uiView.minWidth(100.0)

        val buttonElement = treeElement.element as Button

        val text = buttonElement.text ?: ""

        val uiButton = when (buttonElement.style) {
            Button.ButtonStyle.PRIMARY -> PrimaryButton(text)
            Button.ButtonStyle.SECONDARY -> CommonButton(text)
            Button.ButtonStyle.TEXT -> TextButton(text)
        }.apply {
            val padding = DEFAULT_MARGIN / 2
            layoutMargins = UIEdgeInsets(padding, padding, padding, padding)
        }

        uiButton.titleLabel.setTextLabelProperties(buttonElement)
        uiButton.titleLabel.setAdjustsFontSizeToFitWidth(false)
        uiButton.titleLabel.numberOfLines = 1 // TODO, but does not work on web either
        // how to implement multiline buttons: http://stackoverflow.com/a/34228276/7487013

        uiButton.contentHorizontalAlignment = when (buttonElement.textAlignment) {
            HAlignment.START -> UIControlContentHorizontalAlignment.Left
            HAlignment.CENTER -> UIControlContentHorizontalAlignment.Center
            HAlignment.END -> UIControlContentHorizontalAlignment.Right
            HAlignment.JUSTIFY -> UIControlContentHorizontalAlignment.Center
        }

        uiButton.isEnabled = buttonElement.isEnabled

        uiButton.addOnTouchUpInsideListener { _, _ ->
            mosaikViewController.lastViewInteracted = uiButton
            treeElement.clicked()
        }

        // the actual button is wrapped in an outer view to have a little padding
        // like on other platforms
        uiView.addSubview(uiButton)
        val padding = DEFAULT_MARGIN / 2
        uiView.layoutMargins = UIEdgeInsets(padding, padding, padding, padding)
        uiButton.edgesToSuperview()
    }

    override val handlesClicks: Boolean get() = true
}

class MarkDownHolder(treeElement: TreeElement) : UiViewHolder(UITextView(CGRect.Zero()), treeElement) {
    init {
        val mosaikViewElement = treeElement.element as MarkDown

        val flavour = CommonMarkFlavourDescriptor()
        val markDownContent = mosaikViewElement.content
        val generatedHtmlContent = HtmlGenerator(
            markDownContent,
            MarkdownParser(flavour).buildMarkdownTreeFromString(markDownContent),
            flavour
        ).generateHtml()

        (uiView as UITextView).apply {
            font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)

            // html is generated with <body><p>...</p></body> frame
            // we have to change to <span ...>...</span> to not render unnecessary vertical space
            val htmlContent = generatedHtmlContent.substringAfter("<body><p>").substringBeforeLast("</p></body>")
            val renderHtmlContent =
                "<span style=\"font-family: '-apple-system', 'HelveticaNeue'; font-size: ${font.pointSize}\">$htmlContent</span>"

            setHtmlText(renderHtmlContent)
            textAlignment = mosaikViewElement.contentAlignment.toTextAlignment()
        }
    }
}

class TokenLabelHolder(treeElement: TreeElement) : UiViewHolder(UIStackView(), treeElement) {

    private val genuineImageContainer = GenuineImageContainer()
    private val thumbnailContainer = ThumbnailContainer(22.0)

    private val valAndName: ThemedLabel
    private val stackView = uiView as UIStackView
    private var tokenInfo: TokenInformation? = null
    private val mosaikElement = treeElement.element as TokenLabel
    private val decorated = mosaikElement.isDecorated

    init {
        stackView.alignment = UIStackViewAlignment.Center

        valAndName = mosaikElement.style.toUiLabelView().apply {
            numberOfLines = 1
            enforceKeepIntrinsicWidth()
        }

        if (decorated)
            stackView.addArrangedSubview(thumbnailContainer)
        stackView.addArrangedSubview(valAndName)
        if (decorated)
            stackView.addArrangedSubview(genuineImageContainer)

        treeElement.viewTree.registerJobFor(treeElement) {
            val appDelegate = getAppDelegate()
            withContext(Dispatchers.IO) {
                TokenInfoManager.getInstance().getTokenInformationFlow(
                    mosaikElement.tokenId,
                    appDelegate.database.tokenDbProvider,
                    ApiServiceManager.getOrInit(appDelegate.prefs)
                ).collect {
                    tokenInfo = it
                    runOnMainThread { refresh() }
                }
            }
        }

        refresh()
    }

    private fun refresh() {
        val amountText = mosaikElement.amount?.let {
            TokenAmount(it, tokenInfo?.decimals ?: mosaikElement.decimals).toString()
        }
        val tokenName = tokenInfo?.displayName ?: mosaikElement.tokenName ?: getAppDelegate().texts.get(
            STRING_LABEL_UNNAMED_TOKEN
        )

        valAndName.text = if (!amountText.isNullOrBlank()) amountText + " " + tokenName else tokenName

        if (decorated) {
            genuineImageContainer.setGenuineFlag(tokenInfo?.genuineFlag ?: GENUINE_UNKNOWN)
            thumbnailContainer.setThumbnail(tokenInfo?.thumbnailType ?: THUMBNAIL_TYPE_NONE)
        }
    }
}