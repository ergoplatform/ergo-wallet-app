package org.ergoplatform.ios.settings

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.WalletStateSyncManager
import org.ergoplatform.ios.CrashHandler
import org.ergoplatform.ios.Preferences
import org.ergoplatform.ios.api.IosAuthentication
import org.ergoplatform.ios.ui.*
import org.ergoplatform.uilogic.*
import org.ergoplatform.uilogic.settings.SettingsUiLogic
import org.robovm.apple.coregraphics.CGPoint
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSArray
import org.robovm.apple.localauthentication.LAContext
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
        compiledBy.text = texts.format(STRING_DESC_ABOUT, texts.get(STRING_ABOUT_YEAR))
        compiledBy.textAlignment = NSTextAlignment.Center

        val moreInfo = UITextView(CGRect.Zero()).apply {
            setHtmlText(texts.get(STRING_DESC_ABOUT_MOREINFO))
            textAlignment = NSTextAlignment.Center
            font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)
        }


        // Fiat currency setting
        val preferences = getAppDelegate().prefs
        val fiatCurrencyContainer = buildFiatCurrencySettings(texts, preferences)


        // Debug information

        val showDebugInfoButton = TextButton(texts.get(STRING_BUTTON_SHOW_DEBUG_INFO))
        showDebugInfoButton.addOnTouchUpInsideListener { _, _ ->
            val navController = UINavigationController(DebugInfoViewController())
            navController.modalPresentationStyle = UIModalPresentationStyle.FormSheet
            this.presentViewController(navController, true) { }
        }


        // Expert settings
        val expertSettingsContainer = buildExpertSettings(texts)

        // NFT settings
        val nftSettingsContainer = buildNftSettings(texts, preferences)

        // App Lock settings
        val appLockSettingsContainer = buildAppLockSettings(texts, preferences)

        // Containers, StackView, Scrollview

        val container = UIView()
        val stackView =
            UIStackView(
                NSArray(
                    ergoLogo,
                    title,
                    version,
                    compiledBy,
                    moreInfo,
                    createHorizontalSeparator(),
                    fiatCurrencyContainer,
                    createHorizontalSeparator(),
                    nftSettingsContainer,
                    createHorizontalSeparator(),
                    appLockSettingsContainer,
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
        scrollView.edgesToSuperview(true)
        scrollView.setDelaysContentTouches(false)
    }

    private fun buildFiatCurrencySettings(texts: I18NBundle, preferences: Preferences): UIView {
        val fiatCurrencyContainer = UIView()
        val changeFiatCurrencyButton =
            TextButton(uiLogic.getFiatCurrencyButtonText(preferences, IosStringProvider(texts)))
        changeFiatCurrencyButton.addOnTouchUpInsideListener { _, _ ->
            presentViewController(DisplayCurrencyListViewController { currency ->
                preferences.prefDisplayCurrency = currency
                WalletStateSyncManager.getInstance().invalidateCache(resetFiatValue = true)
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

    private fun buildNftSettings(texts: I18NBundle, preferences: Preferences): UIView {
        val container = UIView(CGRect.Zero())
        val titleNftSettings = Body1BoldLabel().apply {
            text = texts.get(STRING_DESC_TOKEN_SETTINGS)
            textAlignment = NSTextAlignment.Center
        }
        val descTokenVerification = UITextView(CGRect.Zero()).apply {
            setHtmlText(texts.get(STRING_DESC_TOKEN_VERIFICATION))
            textAlignment = NSTextAlignment.Center
            font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)
        }
        val descNftDownload = Body1Label().apply {
            text = texts.get(STRING_DESC_DOWNLOAD_CONTENT)
            textAlignment = NSTextAlignment.Center
        }
        val getButtonLabel = {
            texts.get(
                if (preferences.downloadNftContent) STRING_BUTTON_DOWNLOAD_CONTENT_OFF
                else STRING_BUTTON_DOWNLOAD_CONTENT_ON
            )
        }
        val button = TextButton(getButtonLabel())
        button.addOnTouchUpInsideListener { _, _ ->
            preferences.downloadNftContent = !preferences.downloadNftContent
            button.setTitle(getButtonLabel(), UIControlState.Normal)
        }

        container.addSubview(titleNftSettings)
        container.addSubview(descTokenVerification)
        container.addSubview(descNftDownload)
        container.addSubview(button)
        titleNftSettings.topToSuperview(topInset = DEFAULT_MARGIN).widthMatchesSuperview()
        descTokenVerification.topToBottomOf(titleNftSettings, DEFAULT_MARGIN)
            .widthMatchesSuperview()
        descNftDownload.topToBottomOf(descTokenVerification, DEFAULT_MARGIN * 2.5).widthMatchesSuperview()
        button.bottomToSuperview().topToBottomOf(descNftDownload, inset = DEFAULT_MARGIN)
            .widthMatchesSuperview()
        return container
    }

    private fun buildAppLockSettings(texts: I18NBundle, preferences: Preferences): UIView {
        val container = UIView(CGRect.Zero())
        val titleAppLock = Body1BoldLabel().apply {
            text = texts.get(STRING_TITLE_APP_LOCK)
            textAlignment = NSTextAlignment.Center
        }
        val descAppLock = UITextView(CGRect.Zero()).apply {
            setHtmlText(texts.get(STRING_DESC_APP_LOCK))
            textAlignment = NSTextAlignment.Center
            font = UIFont.getSystemFont(FONT_SIZE_BODY1, UIFontWeight.Regular)
        }
        val getButtonLabel = {
            texts.get(
                if (preferences.enableAppLock) STRING_BUTTON_DOWNLOAD_CONTENT_OFF
                else STRING_BUTTON_DOWNLOAD_CONTENT_ON
            )
        }
        val button = TextButton(getButtonLabel())
        button.addOnTouchUpInsideListener { _, _ ->
            IosAuthentication.authenticate(getAppDelegate().texts.get(STRING_BUTTON_UNLOCK),
                object : IosAuthentication.IosAuthCallback {
                    override fun onAuthenticationSucceeded(context: LAContext) {
                        runOnMainThread {
                            preferences.enableAppLock = !preferences.enableAppLock
                            button.setTitle(getButtonLabel(), UIControlState.Normal)
                        }
                    }

                    override fun onAuthenticationError(error: String) {

                    }

                    override fun onAuthenticationCancelled() {

                    }

                }
            )
        }

        container.addSubview(titleAppLock)
        container.addSubview(descAppLock)
        container.addSubview(button)
        titleAppLock.topToSuperview(topInset = DEFAULT_MARGIN).widthMatchesSuperview()
        descAppLock.topToBottomOf(titleAppLock, DEFAULT_MARGIN)
            .widthMatchesSuperview()
        button.bottomToSuperview().topToBottomOf(descAppLock, inset = DEFAULT_MARGIN)
            .widthMatchesSuperview()
        return container
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
            presentViewController(
                UINavigationController(ConnectionSettingsViewController()),
                true
            ) {}
        }

        expertSettingsContainer.addSubviews(listOf(title, button))
        title.topToSuperview(topInset = DEFAULT_MARGIN).widthMatchesSuperview()
        button.bottomToSuperview().topToBottomOf(title, inset = DEFAULT_MARGIN)
            .widthMatchesSuperview()

        return expertSettingsContainer
    }

    class DebugInfoViewController : UIViewController() {
        private lateinit var textView: UITextView

        override fun viewDidLoad() {
            super.viewDidLoad()
            title = getAppDelegate().texts.get(STRING_BUTTON_SHOW_DEBUG_INFO)
            val lastCrashInformation = CrashHandler.getLastCrashInformation()

            lastCrashInformation?.let {
                val uiBarButtonItem = UIBarButtonItem(UIBarButtonSystemItem.Action)
                uiBarButtonItem.setOnClickListener {
                    shareText(
                        lastCrashInformation,
                        uiBarButtonItem
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