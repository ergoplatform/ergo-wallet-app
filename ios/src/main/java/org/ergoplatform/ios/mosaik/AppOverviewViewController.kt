package org.ergoplatform.ios.mosaik

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.ergoplatform.ios.IosCacheManager
import org.ergoplatform.ios.ui.*
import org.ergoplatform.ios.wallet.WIDTH_ICONS
import org.ergoplatform.mosaik.MosaikAppEntry
import org.ergoplatform.mosaik.MosaikAppOverviewUiLogic
import org.ergoplatform.mosaik.MosaikAppSuggestion
import org.ergoplatform.uilogic.*
import org.ergoplatform.utils.LogUtils
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.foundation.NSData
import org.robovm.apple.uikit.*

class AppOverviewViewController : ViewControllerWithKeyboardLayoutGuide() {
    private val uiLogic = IosMosaikAppOverviewUiLogic()

    private lateinit var inputAddress: UITextField
    private lateinit var favoritesList: AppItemStackView
    private lateinit var lastVisitedList: AppItemStackView
    private lateinit var suggestionsList: AppItemStackView
    private lateinit var suggestionsLabel: UILabel

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()
        navigationController.navigationBar?.tintColor = UIColor.label()

        val texts = getAppDelegate().texts

        title = texts.get(STRING_TITLE_MOSAIK)

        inputAddress = EndIconTextField().apply {
            returnKeyType = UIReturnKeyType.Continue
            placeholder = texts.get(STRING_HINT_APP_URL)
            textContentType = UITextContentType.URL
            autocapitalizationType = UITextAutocapitalizationType.None
            autocorrectionType = UITextAutocorrectionType.No

            delegate = object : UITextFieldDelegateAdapter() {
                override fun shouldReturn(textField: UITextField?): Boolean {
                    navigateToApp()
                    return true
                }
            }

            setCustomActionField(getIosSystemImage(IMAGE_ARROW_RIGHT, UIImageSymbolScale.Small)!!) {
                navigateToApp()
            }
        }

        val container = UIView()
        container.addSubview(inputAddress)

        inputAddress.topToSuperview(topInset = DEFAULT_MARGIN)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)

        val favoritesLabel = Body1BoldLabel().apply {
            text = texts.get(STRING_TITLE_FAVORITES)
            textColor = uiColorErgo
        }
        container.addSubview(favoritesLabel)
        favoritesLabel.topToBottomOf(inputAddress, DEFAULT_MARGIN * 3)
            .widthMatchesSuperview(inset = DEFAULT_MARGIN, maxWidth = MAX_WIDTH)

        favoritesList = AppItemStackView()
        container.addSubview(favoritesList)
        favoritesList.topToBottomOf(favoritesLabel).leftToLeftOf(favoritesLabel, DEFAULT_MARGIN)
            .rightToRightOf(favoritesLabel, DEFAULT_MARGIN)

        val lastVisitedLabel = Body1BoldLabel().apply {
            text = texts.get(STRING_TITLE_LAST_VISITED)
            textColor = uiColorErgo
        }
        container.addSubview(lastVisitedLabel)
        lastVisitedLabel.topToBottomOf(favoritesList, DEFAULT_MARGIN * 3).leftToLeftOf(favoritesLabel)
            .rightToRightOf(favoritesLabel)
        lastVisitedList = AppItemStackView()
        container.addSubview(lastVisitedList)
        lastVisitedList.topToBottomOf(lastVisitedLabel).leftToLeftOf(favoritesList).rightToRightOf(favoritesList)

        suggestionsLabel = Body1BoldLabel().apply {
            text = texts.get(STRING_TITLE_SUGGESTIONS)
            textColor = uiColorErgo
            isHidden = true
        }
        container.addSubview(suggestionsLabel)
        suggestionsLabel.topToBottomOf(lastVisitedList, DEFAULT_MARGIN * 3).leftToLeftOf(favoritesLabel)
            .rightToRightOf(favoritesLabel)
        suggestionsList = AppItemStackView()
        container.addSubview(suggestionsList)
        suggestionsList.topToBottomOf(suggestionsLabel).leftToLeftOf(favoritesList).rightToRightOf(favoritesList)
        suggestionsList.bottomToSuperview()
        suggestionsList.isHidden = true

        // TODO disclaimer, opt in

        val scrollView = container.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.topToSuperview()
            .widthMatchesSuperview()
            .bottomToKeyboard(this, DEFAULT_MARGIN)
        scrollView.setDelaysContentTouches(false)
    }

    private fun navigateToApp() {
        navigateToApp(inputAddress.text, null)
        inputAddress.text = ""
    }

    private fun navigateToApp(appUrl: String, appName: String?) {
        navigationController.pushViewController(
            MosaikViewController(appUrl, appName), true
        )
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        val db = getAppDelegate().database
        val setupFlows = uiLogic.init(db)
        if (!setupFlows) uiLogic.setupDbFlows(db)
        viewControllerScope.launch {
            uiLogic.favoritesFlow.collectLatest {
                runOnMainThread {
                    favoritesList.updateList(it)
                }
            }
        }
        viewControllerScope.launch {
            uiLogic.lastVisitedFlow.collectLatest {
                runOnMainThread {
                    lastVisitedList.updateList(it)
                }
            }
        }
        viewControllerScope.launch {
            uiLogic.suggestionFlow.collectLatest {
                runOnMainThread {
                    suggestionsList.updateList(it)
                    suggestionsList.isHidden = it.isEmpty()
                    suggestionsLabel.isHidden = it.isEmpty()
                }
            }
        }
    }

    private inner class IosMosaikAppOverviewUiLogic : MosaikAppOverviewUiLogic() {
        override val coroutineScope: CoroutineScope
            get() = viewControllerScope
    }

    inner class AppItemStackView : UIStackView(CGRect.Zero()) {
        init {
            axis = UILayoutConstraintAxis.Vertical
            spacing = DEFAULT_MARGIN
        }

        fun updateList(list: List<Any>) {
            this.clearArrangedSubviews()

            if (list.isEmpty()) {
                addArrangedSubview(Body1Label().apply {
                    text = getAppDelegate().texts.get(STRING_DESC_NONE)
                })
            }

            list.forEach { appEntry ->
                if (appEntry is MosaikAppEntry)
                    addArrangedSubview(AppItem().bind(appEntry))
                else if (appEntry is MosaikAppSuggestion)
                    addArrangedSubview(AppItem().bind(appEntry))
            }
        }

    }

    inner class AppItem : CardView() {
        private val titleLabel = Body1BoldLabel()
        private val descLabel = Body2Label()
        private val appIconImage = UIImageView()
        private var appUrl: String? = null
        private var appName: String? = null

        init {
            layoutMargins = UIEdgeInsets.Zero()
            titleLabel.textColor = uiColorErgo
            appIconImage.contentMode = UIViewContentMode.ScaleAspectFit

            addSubview(titleLabel)
            addSubview(descLabel)
            addSubview(appIconImage)

            appIconImage.centerVertical().topToSuperview(topInset = DEFAULT_MARGIN, canBeMore = true)
                .bottomToSuperview(bottomInset = DEFAULT_MARGIN, canBeLess = true)
                .leftToSuperview(inset = DEFAULT_MARGIN)
                .fixedWidth(WIDTH_ICONS).fixedHeight(WIDTH_ICONS)
            titleLabel.topToSuperview(topInset = DEFAULT_MARGIN).leftToRightOf(appIconImage, DEFAULT_MARGIN)
                .rightToSuperview(inset = DEFAULT_MARGIN)
            descLabel.topToBottomOf(titleLabel).bottomToSuperview(bottomInset = DEFAULT_MARGIN).leftToLeftOf(titleLabel)
                .rightToRightOf(titleLabel)

            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                appUrl?.let {
                    navigateToApp(appUrl!!, appName)
                }
            })
        }

        fun bind(mosaikApp: MosaikAppEntry): CardView {
            bind(
                mosaikApp.name,
                (if (mosaikApp.description.isNullOrBlank()) mosaikApp.url else mosaikApp.description)!!,
                mosaikApp.url,
                try {
                    mosaikApp.iconFile?.let { fileId ->
                        IosCacheManager().readFileContent(fileId)?.let { bytes -> UIImage(NSData(bytes)) }
                    }
                } catch (t: Throwable) {
                    LogUtils.logDebug("MosaikOverview", "Error accessing icon file", t)
                    null
                }
            )
            return this
        }

        fun bind(mosaikAppSuggestion: MosaikAppSuggestion): CardView {
            bind(
                mosaikAppSuggestion.appName,
                mosaikAppSuggestion.appDescription,
                mosaikAppSuggestion.appUrl,
                image = null
            )
            return this
        }

        fun bind(title: String, description: String, url: String, image: UIImage?) {
            titleLabel.text = title
            descLabel.text = description
            this.appName = title
            this.appUrl = url
            descLabel.numberOfLines = if (url == description) 1 else 3

            appIconImage.image = image ?: getIosSystemImage(IMAGE_MOSAIK, UIImageSymbolScale.Medium)
            appIconImage.tintColor = if (image == null) UIColor.label() else null
        }
    }
}