package org.ergoplatform.ios.addressbook

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.ergoplatform.addressbook.sortedByLabel
import org.ergoplatform.ios.ui.*
import org.ergoplatform.persistance.AddressBookEntry
import org.ergoplatform.persistance.IAddressWithLabel
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.uilogic.*
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
        // TODO
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

        } else {
            // TODO
        }
    }

    private fun refreshOwnAddresses(walletList: List<Wallet>) {
        ownAddressesStack.clearArrangedSubviews()
        walletList.forEach { wallet ->
            val addresses = wallet.getSortedDerivedAddressesList()

            if (addresses.size == 1) {
                val entry = AddressEntry(addresses.first(), wallet.walletConfig.displayName)
                entry.isUserInteractionEnabled = true
                entry.addGestureRecognizer(UITapGestureRecognizer {
                    addressChosen(addresses.first())
                })
                ownAddressesStack.addArrangedSubview(entry)
            } else {
                // TODO
            }
        }
    }

    private fun addressChosen(address: IAddressWithLabel) {
        dismissViewController(true) { onChooseEntry(address) }
    }

    inner class AddressEntry(addressEntry: IAddressWithLabel, fallBackLabel: String?) : UIStackView() {
        init {
            axis = UILayoutConstraintAxis.Vertical
            isLayoutMarginsRelativeArrangement = true
            layoutMargins = UIEdgeInsets(DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN)

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
        }
    }
}