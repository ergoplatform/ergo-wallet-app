package org.ergoplatform.ios.settings

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.NodeConnector
import org.ergoplatform.ios.ui.*
import org.ergoplatform.settings.SettingsUiLogic
import org.ergoplatform.uilogic.*
import org.robovm.apple.coregraphics.CGPoint
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.*
import org.robovm.apple.uikit.*

class SettingsViewController : CoroutineViewController() {

    private val uiLogic = SettingsUiLogic()

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        val texts = getAppDelegate().texts

        // Header

        val ergoLogo = UIImageView(ergoLogoImage.imageWithTintColor(UIColor.label()))
        ergoLogo.contentMode = UIViewContentMode.ScaleAspectFit
        ergoLogo.fixedHeight(100.0)

        val title = Headline1Label()
        title.text = texts.get(STRING_APP_NAME)
        title.textAlignment = NSTextAlignment.Center
        val version = Body2Label()
        version.text = CrashHandler.getAppVersion()
        version.textAlignment = NSTextAlignment.Center
        val compiledBy = Body1Label()
        compiledBy.text = texts.get(STRING_DESC_ABOUT)
        compiledBy.textAlignment = NSTextAlignment.Center

        val moreInfo = UITextView(CGRect.Zero()).apply {
            setHtmlText(texts.get(STRING_DESC_ABOUT_MOREINFO))
            textColor = UIColor.label()
            tintColor = uiColorErgo
            textAlignment = NSTextAlignment.Center
            font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)
        }


        // Fiat currency setting
        val fiatCurrencyContainer = buildFiatCurrencySettings(texts)


        // Debug information

        val showDebugInfoButton = TextButton(texts.get(STRING_BUTTON_SHOW_DEBUG_INFO))
        showDebugInfoButton.addOnTouchUpInsideListener { _, _ ->
            val navController = UINavigationController(DebugInfoViewController())
            navController.modalPresentationStyle = UIModalPresentationStyle.FormSheet
            this.presentViewController(navController, true) { }
        }


        // Expert settings
        val expertSettingsContainer = buildExpertSettings(texts)


        // Containers, Stackview, Scrollview

        val container = UIView()
        val stackView =
            UIStackView(
                NSArray(
                    ergoLogo,
                    title,
                    version,
                    moreInfo,
                    createHorizontalSeparator(),
                    fiatCurrencyContainer,
                    createHorizontalSeparator(),
                    expertSettingsContainer,
                    createHorizontalSeparator(),
                    showDebugInfoButton
                )
            )
        stackView.axis = UILayoutConstraintAxis.Vertical
        stackView.spacing = DEFAULT_MARGIN
        val scrollView = container.wrapInVerticalScrollView()
        container.addSubview(stackView)
        stackView.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)
            .bottomToSuperview(bottomInset = DEFAULT_MARGIN)

        view.addSubview(scrollView)
        scrollView.edgesToSuperview()
        scrollView.setDelaysContentTouches(false)
    }

    private fun buildFiatCurrencySettings(texts: I18NBundle): UIView {
        val fiatCurrencyContainer = UIView()
        val preferences = getAppDelegate().prefs
        val changeFiatCurrencyButton =
            TextButton(uiLogic.getFiatCurrencyButtonText(preferences, IosStringProvider(texts)))
        changeFiatCurrencyButton.addOnTouchUpInsideListener { _, _ ->
            presentViewController(DisplayCurrencyListViewController { currency ->
                preferences.prefDisplayCurrency = currency
                NodeConnector.getInstance().invalidateCache()
                changeFiatCurrencyButton.setTitle(
                    uiLogic.getFiatCurrencyButtonText(
                        preferences,
                        IosStringProvider(texts)
                    ),
                    UIControlState.Normal
                )
            }, true) { }
        }
        val coingeckoLabel = UITextView(CGRect.Zero()).apply {
            setHtmlText(texts.get(STRING_DESC_COINGECKO))
            textColor = UIColor.label()
            tintColor = uiColorErgo
            textAlignment = NSTextAlignment.Center
            font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)
        }
        fiatCurrencyContainer.addSubview(coingeckoLabel)
        fiatCurrencyContainer.addSubview(changeFiatCurrencyButton)
        changeFiatCurrencyButton.widthMatchesSuperview(inset = DEFAULT_MARGIN * 2).bottomToSuperview()
            .topToBottomOf(coingeckoLabel)
        coingeckoLabel.topToSuperview().widthMatchesSuperview()
        return fiatCurrencyContainer
    }

    private fun buildExpertSettings(texts: I18NBundle): UIView {
        val expertSettingsContainer = UIView(CGRect.Zero())

        val title = Body1Label().apply {
            text = texts.get(STRING_DESC_EXPERT_SETTINGS)
            numberOfLines = 1
            textAlignment = NSTextAlignment.Center
        }

        val button = TextButton(texts.get(STRING_BUTTON_CONNECTION_SETTINGS))
        button.addOnTouchUpInsideListener { _, _ ->
            presentViewController(UINavigationController(ConnectionSettingsViewController()), true) {}
        }

        expertSettingsContainer.addSubviews(listOf(title, button))
        title.topToSuperview(topInset = DEFAULT_MARGIN).widthMatchesSuperview()
        button.bottomToSuperview().topToBottomOf(title, inset = DEFAULT_MARGIN).widthMatchesSuperview(

        )

        return expertSettingsContainer
    }

    class DebugInfoViewController : UIViewController() {
        lateinit var textView: UITextView

        override fun viewDidLoad() {
            super.viewDidLoad()
            title = getAppDelegate().texts.get(STRING_BUTTON_SHOW_DEBUG_INFO)
            val lastCrashInformation = CrashHandler.getLastCrashInformation()

            lastCrashInformation?.let {
                val uiBarButtonItem = UIBarButtonItem(UIBarButtonSystemItem.Action)
                uiBarButtonItem.setOnClickListener {
                    shareText(
                        lastCrashInformation,
                        uiBarButtonItem.keyValueCoder.getValue("view") as UIView
                    )
                }
                navigationItem.rightBarButtonItem = uiBarButtonItem
            }
            navigationController.navigationBar?.tintColor = uiColorErgo
            val closeButton = UIBarButtonItem(UIBarButtonSystemItem.Close)
            navigationItem.leftBarButtonItem = closeButton
            closeButton.setOnClickListener { dismissViewController(true) {} }

            textView = createTextview()
            textView.isEditable = false
            textView.text = lastCrashInformation ?: "-"

            view.addSubview(textView)
            textView.edgesToSuperview(inset = DEFAULT_MARGIN * 2)
            view.backgroundColor = UIColor.systemBackground()

        }

        override fun viewDidLayoutSubviews() {
            super.viewDidLayoutSubviews()
            textView.setContentOffset(CGPoint.Zero(), false)
        }
    }
}