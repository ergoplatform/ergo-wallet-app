package org.ergoplatform.ios.settings

import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.STRING_APP_NAME
import org.ergoplatform.uilogic.STRING_DESC_ABOUT
import org.ergoplatform.uilogic.STRING_DESC_ABOUT_MOREINFO
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.*
import org.robovm.apple.uikit.*

class SettingsViewController : CoroutineViewController() {

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        val texts = getAppDelegate().texts

        val ergoLogo = UIImageView(ergoLogoImage.imageWithTintColor(UIColor.label()))
        ergoLogo.contentMode = UIViewContentMode.ScaleAspectFit
        ergoLogo.fixedHeight(100.0)

        val title = Headline1Label()
        title.text = texts.get(STRING_APP_NAME)
        title.textAlignment = NSTextAlignment.Center
        val version = Body2Label()
        version.text = NSBundle.getMainBundle().infoDictionary?.getString("CFBundleShortVersionString")
        version.textAlignment = NSTextAlignment.Center
        val compiledBy = Body1Label()
        compiledBy.text = texts.get(STRING_DESC_ABOUT)
        compiledBy.textAlignment = NSTextAlignment.Center

        val moreInfo = UITextView(CGRect.Zero())
        moreInfo.setHtmlText(texts.get(STRING_DESC_ABOUT_MOREINFO))
        moreInfo.textColor = UIColor.label()
        moreInfo.textAlignment = NSTextAlignment.Center
        moreInfo.font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)

        val container = UIView()
        val stackView = UIStackView(NSArray(ergoLogo, title, version, moreInfo, getHorizontalSeparator()))
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.spacing = DEFAULT_MARGIN
        val scrollView = container.wrapInVerticalScrollView()
        container.addSubview(stackView)
        stackView.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)

        view.addSubview(scrollView)
        scrollView.edgesToSuperview()
    }
}