package org.ergoplatform.ios.addressbook

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.ergoplatform.addressbook.sortedByLabel
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.AddressBookEntry
import org.ergoplatform.persistance.IAddressWithLabel
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.*
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.getSortedDerivedAddressesList
import org.ergoplatform.wallet.sortedByDisplayName
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

class ChooseAddressDialogViewController(
    private val onChooseEntry: (IAddressWithLabel) -> Unit
) : CoroutineViewController() {

    private lateinit var ownAddressesStack: UIStackView
    private lateinit var addressBookStack: UIStackView

    override fun viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = UIColor.systemBackground()

        val closeButton = addCloseButton()

        val outerStack = UIStackView().apply {
            axis = UILayoutConstraintAxis.Vertical
        }

        val texts = getAppDelegate().texts
        val ownAddressesTitleLabel = Body1BoldLabel().apply {
            text = texts.get(STRING_LABEL_OWN_ADDRESSES)
            textAlignment = NSTextAlignment.Center
            textColor = uiColorErgo
        }

        ownAddressesStack = UIStackView().apply {
            axis = UILayoutConstraintAxis.Vertical
            alignment = UIStackViewAlignment.Center
            isLayoutMarginsRelativeArrangement = true
            layoutMargins = UIEdgeInsets(DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN)
        }

        val addressBookTitleLabel = Body1BoldLabel().apply {
            text = texts.get(STRING_LABEL_SAVED_ADDRESSES)
            textAlignment = NSTextAlignment.Center
            textColor = uiColorErgo
        }

        addressBookStack = UIStackView().apply {
            axis = UILayoutConstraintAxis.Vertical
            alignment = UIStackViewAlignment.Center
            isLayoutMarginsRelativeArrangement = true
            layoutMargins = UIEdgeInsets(DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN)
        }

        val addNewAbEntryButton = TextButton(texts.get(STRING_BUTTON_ADD_ADDRESS_ENTRY)).apply {
            addOnTouchUpInsideListener { _, _ ->
                onEditEntry(null)
            }
            setTitleColor(UIColor.label(), UIControlState.Normal)
        }

        outerStack.apply {
            addArrangedSubview(ownAddressesTitleLabel)
            addArrangedSubview(ownAddressesStack)
            val separator = createHorizontalSeparator()
            addArrangedSubview(separator)
            addArrangedSubview(addressBookTitleLabel)
            addArrangedSubview(addNewAbEntryButton)
            addArrangedSubview(addressBookStack)
            setCustomSpacing(DEFAULT_MARGIN, ownAddressesStack)
            setCustomSpacing(DEFAULT_MARGIN, addressBookTitleLabel)
            setCustomSpacing(DEFAULT_MARGIN, addNewAbEntryButton)
            setCustomSpacing(DEFAULT_MARGIN * 3, separator)
        }

        val content = UIView(CGRect.Zero())
        content.addSubview(outerStack)
        outerStack.edgesToSuperview(maxWidth = MAX_WIDTH)
        val scrollView = content.wrapInVerticalScrollView()
        view.addSubview(scrollView)
        scrollView.topToBottomOf(closeButton).bottomToSuperview(true).widthMatchesSuperview(true)

    }

    private fun onEditEntry(addressBookEntry: AddressBookEntry?) {
        presentViewController(EditAddressDialogViewController(addressBookEntry), true) {}
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        viewControllerScope.launch {
            getAppDelegate().database.walletDbProvider.getWalletsWithStatesFlow().collectLatest { walletList ->
                runOnMainThread {
                    refreshOwnAddresses(walletList.sortedByDisplayName())
                }
            }
        }
        viewControllerScope.launch {
            getAppDelegate().database.addressBookDbProvider.getAllAddressEntries().collectLatest { addressesList ->
                runOnMainThread {
                    refreshAddressBook(addressesList.sortedByLabel())
                }
            }
        }
    }

    private fun refreshAddressBook(addressBookEntries: List<AddressBookEntry>) {
        addressBookStack.clearArrangedSubviews()

        if (addressBookEntries.isEmpty()) {

            addressBookStack.addArrangedSubview(
                Body1Label().apply {
                    text = getAppDelegate().texts.get(STRING_LABEL_NO_ENTRIES)
                    textAlignment = NSTextAlignment.Center
                }
            )

        } else addressBookEntries.forEach { abEntry ->
            val entryView = AddressEntry(abEntry)
            entryView.addGestureRecognizer(UILongPressGestureRecognizer {
                onEditEntry(abEntry)
            })
            addressBookStack.addArrangedSubview(entryView)
        }
    }

    private fun refreshOwnAddresses(walletList: List<Wallet>) {
        ownAddressesStack.clearArrangedSubviews()
        walletList.forEach { wallet ->
            val addresses = wallet.getSortedDerivedAddressesList()

            if (addresses.size == 1) {
                val entry = AddressEntry(addresses.first(), wallet.walletConfig.displayName)
                ownAddressesStack.addArrangedSubview(entry)
            } else {
                val walletContainer = WalletAddressesContainer(wallet, addresses)
                ownAddressesStack.addArrangedSubview(walletContainer)
            }
        }
    }

    private fun addressChosen(address: IAddressWithLabel) {
        dismissViewController(true) { onChooseEntry(address) }
    }

    inner class WalletAddressesContainer(
        wallet: Wallet,
        sortedAddresses: List<WalletAddress>
    ) : UIStackView() {
        private val walletTitleLabel = Body1BoldLabel().apply {
            text = wallet.walletConfig.displayName
            textAlignment = NSTextAlignment.Center
            lineBreakMode = NSLineBreakMode.TruncatingTail
            numberOfLines = 1
        }
        private val expandImageView = UIImageView().apply {
            tintColor = UIColor.label()
        }
        private val titleContainer = UIView(CGRect.Zero())
        private var expanded = false

        init {
            axis = UILayoutConstraintAxis.Vertical
            titleContainer.addSubview(expandImageView)
            titleContainer.addSubview(walletTitleLabel)
            walletTitleLabel.superViewWrapsHeight().leftToSuperview()
            expandImageView.centerVerticallyTo(walletTitleLabel).rightToSuperview()
                .leftToRightOf(walletTitleLabel).enforceKeepIntrinsicWidth()
            titleContainer.isUserInteractionEnabled = true
            titleContainer.addGestureRecognizer(UITapGestureRecognizer {
                expanded = !expanded
                animateLayoutChanges {
                    arrangedSubviews.forEach {
                        it.isHidden = !expanded && !(it === titleContainer)
                    }
                    setImage()
                }
            })

            addArrangedSubview(titleContainer)

            val stringProvider = IosStringProvider(getAppDelegate().texts)
            sortedAddresses.forEach {
                val entryView = AddressEntry(
                    it, it.getAddressLabel(stringProvider),
                    horizontalMargins = DEFAULT_MARGIN * 4
                )
                entryView.isHidden = true
                addArrangedSubview(entryView)
            }

            setImage()
        }

        private fun setImage() {
            expandImageView.image = getIosSystemImage(
                if (expanded) IMAGE_CHEVRON_UP else IMAGE_CHEVRON_DOWN,
                UIImageSymbolScale.Small,
                20.0
            )
        }
    }

    inner class AddressEntry(
        addressEntry: IAddressWithLabel,
        fallBackLabel: String? = null,
        horizontalMargins: Double = DEFAULT_MARGIN,
    ) : UIStackView() {
        init {
            axis = UILayoutConstraintAxis.Vertical
            isLayoutMarginsRelativeArrangement = true
            layoutMargins = UIEdgeInsets(DEFAULT_MARGIN, horizontalMargins, DEFAULT_MARGIN, horizontalMargins)

            (addressEntry.label ?: fallBackLabel)?.let { label ->

                addArrangedSubview(Body1BoldLabel().apply {
                    numberOfLines = 1
                    lineBreakMode = NSLineBreakMode.TruncatingTail
                    text = label
                    textAlignment = NSTextAlignment.Center
                })

            }

            addArrangedSubview(Body2Label().apply {
                numberOfLines = 1
                lineBreakMode = NSLineBreakMode.TruncatingMiddle
                text = addressEntry.address
                textAlignment = NSTextAlignment.Center
            })

            isUserInteractionEnabled = true
            addGestureRecognizer(UITapGestureRecognizer {
                addressChosen(addressEntry)
            })

        }
    }
}