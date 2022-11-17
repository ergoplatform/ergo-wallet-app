package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.*

/**
 * Shows transaction info with boxes and tokens to spend and boxes and tokens to issue
 */
abstract class TransactionContainer(
    private val texts: I18NBundle,
    private val vc: UIViewController
) : UIStackView() {
    private val inboxesList = UIStackView().apply {
        axis = UILayoutConstraintAxis.Vertical
    }
    private val outBoxesList = UIStackView().apply {
        axis = UILayoutConstraintAxis.Vertical
    }

    abstract val titleInboxes: String
    abstract val descInboxes: String
    abstract val titleOutboxes: String
    abstract val descOutboxes: String

    init {
        axis = UILayoutConstraintAxis.Vertical
        spacing = DEFAULT_MARGIN * 2
        layoutMargins = UIEdgeInsets(DEFAULT_MARGIN * 2, 0.0, DEFAULT_MARGIN * 2, 0.0)
        isLayoutMarginsRelativeArrangement = true

        this.addArrangedSubview(createHorizontalSeparator())
        val titleInboxesLabel = Body1BoldLabel().apply {
            text = texts.get(titleInboxes)
            textColor = uiColorErgo
        }
        this.addArrangedSubview(titleInboxesLabel)
        this.setCustomSpacing(DEFAULT_MARGIN, titleInboxesLabel)
        this.addArrangedSubview(Body2Label().apply {
            text = texts.get(descInboxes)
        })
        this.addArrangedSubview(inboxesList)
        this.addArrangedSubview(createHorizontalSeparator())
        val titleOutboxesLabel = Body1BoldLabel().apply {
            text = texts.get(titleOutboxes)
            textColor = uiColorErgo
        }
        this.addArrangedSubview(titleOutboxesLabel)
        this.setCustomSpacing(DEFAULT_MARGIN, titleOutboxesLabel)
        this.addArrangedSubview(Body2Label().apply {
            text = texts.get(descOutboxes)
        })
        this.addArrangedSubview(outBoxesList)
    }

    open fun bindTransaction(
        transactionInfo: TransactionInfo,
        tokenClickListener: ((String) -> Unit)?,
        addressLabelHandler: ((String, (String) -> Unit) -> Unit)? = null,
        tokenLabelHandler: ((String, (String) -> Unit) -> Unit)? = null,
    ) {
        inboxesList.clearArrangedSubviews()
        transactionInfo.inputs.forEach { input ->
            inboxesList.addArrangedSubview(
                TransactionBoxEntryView(vc).bindBoxView(
                    input.value,
                    input.address,
                    input.assets,
                    tokenClickListener,
                    addressLabelHandler,
                    tokenLabelHandler,
                    texts
                )
            )
        }
        outBoxesList.clearArrangedSubviews()
        transactionInfo.outputs.forEach { output ->
            outBoxesList.addArrangedSubview(
                TransactionBoxEntryView(vc).bindBoxView(
                    output.value,
                    output.address,
                    output.assets,
                    tokenClickListener,
                    addressLabelHandler,
                    tokenLabelHandler,
                    texts
                )
            )
        }
    }

    fun defaultAddressLabelHandler(coroutineScope: CoroutineScope): (String, (String) -> Unit) -> Unit =
        { address, callback ->
            coroutineScope.launch {
                val appDelegate = getAppDelegate()
                getAddressLabelFromDatabase(
                    appDelegate.database, address,
                    IosStringProvider(appDelegate.texts)
                )?.let { runOnMainThread { callback(it) } }
            }
        }

    fun defaultTokenLabelHandler(coroutineScope: CoroutineScope): (String, (String) -> Unit) -> Unit =
        { tokenId, callback ->
            coroutineScope.launch {
                getAppDelegate().database.tokenDbProvider.loadTokenInformation(tokenId)?.displayName
                    ?.let { runOnMainThread { callback(it) } }
            }
        }
}

open class SigningTransactionContainer(
    private val texts: I18NBundle,
    vc: UIViewController,
    private val onConfirm: () -> Unit
) : TransactionContainer(texts, vc) {

    override val titleInboxes get() = STRING_TITLE_INBOXES
    override val descInboxes get() = STRING_DESC_INBOXES
    override val titleOutboxes get() = STRING_TITLE_OUTBOXES
    override val descOutboxes get() = STRING_DESC_OUTBOXES

    init {
        this.insertArrangedSubview(Body2BoldLabel().apply {
            text = texts.get(STRING_DESC_SIGNING_REQUEST)
        }, 0)

        val signButton = PrimaryButton(texts.get(STRING_LABEL_CONFIRM)).apply {
            addOnTouchUpInsideListener { _, _ ->
                onConfirm()
            }
        }
        val buttonContainer = UIView(CGRect.Zero()).apply {
            addSubview(signButton)
            signButton.fixedWidth(100.0).centerHorizontal().bottomToSuperview()
                .topToSuperview(topInset = DEFAULT_MARGIN * 2)
        }
        this.addArrangedSubview(buttonContainer)
    }
}