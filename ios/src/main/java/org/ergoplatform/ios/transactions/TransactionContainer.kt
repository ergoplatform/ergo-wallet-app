package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.MessageSeverity
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.transactions.reduceBoxes
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
    abstract val descInboxes: String?
    abstract val titleOutboxes: String
    abstract val descOutboxes: String?

    private val titleInboxesLabel = Body1BoldLabel().apply {
        textColor = uiColorErgo
    }
    private val descInboxesLabel = Body2Label()
    private val titleOutboxesLabel = Body1BoldLabel().apply {
        textColor = uiColorErgo
    }
    private val descOutboxesLabel = Body2Label()

    init {
        axis = UILayoutConstraintAxis.Vertical
        spacing = DEFAULT_MARGIN * 2
        layoutMargins = UIEdgeInsets(DEFAULT_MARGIN * 2, 0.0, DEFAULT_MARGIN * 2, 0.0)
        isLayoutMarginsRelativeArrangement = true

        this.addArrangedSubview(createHorizontalSeparator())
        this.addArrangedSubview(titleInboxesLabel)
        this.setCustomSpacing(DEFAULT_MARGIN, titleInboxesLabel)
        this.addArrangedSubview(descInboxesLabel)
        this.addArrangedSubview(inboxesList)
        this.addArrangedSubview(createHorizontalSeparator())
        this.addArrangedSubview(titleOutboxesLabel)
        this.setCustomSpacing(DEFAULT_MARGIN, titleOutboxesLabel)
        this.addArrangedSubview(descOutboxesLabel)
        this.addArrangedSubview(outBoxesList)
    }

    open fun bindTransaction(
        transactionInfo: TransactionInfo,
        tokenClickListener: ((String) -> Unit)?,
        addressLabelHandler: ((String, (String) -> Unit) -> Unit)? = null,
        tokenLabelHandler: ((String, (String) -> Unit) -> Unit)? = null,
    ) {
        inboxesList.clearArrangedSubviews()
        titleInboxesLabel.text = texts.format(titleInboxes, transactionInfo.inputs.size)
        descInboxesLabel.text = descInboxes?.let { texts.get(it) } ?: ""
        titleOutboxesLabel.text = texts.format(titleOutboxes, transactionInfo.outputs.size)
        descOutboxesLabel.text = descOutboxes?.let { texts.get(it) } ?: ""

        transactionInfo.inputs.forEach { input ->
            inboxesList.addArrangedSubview(
                TransactionBoxEntryView(vc).bindBoxView(
                    input.value,
                    input.address,
                    input.assets,
                    input.additionalRegisters,
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
                    output.additionalRegisters,
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

    override val titleInboxes
        get() =
            if (showReduced) STRING_TITLE_INBOXES else STRING_TITLE_INPUTS_SPENT
    override val descInboxes
        get() =
            if (showReduced) STRING_DESC_INBOXES else null
    override val titleOutboxes
        get() =
            if (showReduced) STRING_TITLE_OUTBOXES else STRING_TITLE_OUTPUTS_CREATED
    override val descOutboxes
        get() =
            if (showReduced) STRING_DESC_OUTBOXES else null

    private var showReduced = true
    private var shownTi: TransactionInfo? = null
    private var tokenClickListener: ((String) -> Unit)? = null
    private var addressLabelHandler: ((String, (String) -> Unit) -> Unit)? = null
    private var tokenLabelHandler: ((String, (String) -> Unit) -> Unit)? = null

    private val hintMessageLabel = Body1Label().apply {
        textAlignment = NSTextAlignment.Center
    }
    private val hintMessageContainer = UIView(CGRect.Zero())

    private val switchReducedModeLabel = Body2BoldLabel().apply {
        textAlignment = NSTextAlignment.Center
        textColor = uiColorErgo
        isUserInteractionEnabled = true
        addGestureRecognizer(UITapGestureRecognizer {
            shownTi?.let {
                showReduced = !showReduced
                bindTransaction(it, tokenClickListener, addressLabelHandler, tokenLabelHandler)
            }
        })
    }

    init {
        this.insertArrangedSubview(Body1BoldLabel().apply {
            text = texts.get(STRING_DESC_SIGNING_REQUEST)
            textAlignment = NSTextAlignment.Center
        }, 0)

        this.insertArrangedSubview(hintMessageContainer, 1)
        this.insertArrangedSubview(switchReducedModeLabel, 2)

        hintMessageContainer.addSubview(hintMessageLabel)
        hintMessageLabel.edgesToSuperview()

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

    override fun bindTransaction(
        transactionInfo: TransactionInfo,
        tokenClickListener: ((String) -> Unit)?,
        addressLabelHandler: ((String, (String) -> Unit) -> Unit)?,
        tokenLabelHandler: ((String, (String) -> Unit) -> Unit)?
    ) {
        shownTi = transactionInfo
        this.tokenClickListener = tokenClickListener
        this.addressLabelHandler = addressLabelHandler
        this.tokenLabelHandler = tokenLabelHandler

        switchReducedModeLabel.text = texts.get(
            if (showReduced) STRING_BUTTON_SWITCH_TO_BOXES
            else STRING_BUTTON_SWITCH_TO_AMOUNTS
        )

        val tiToUse = if (showReduced) transactionInfo.reduceBoxes() else transactionInfo

        super.bindTransaction(tiToUse, tokenClickListener, addressLabelHandler, tokenLabelHandler)

        tiToUse.hintMsg?.let { (message, severity) ->
            hintMessageLabel.text = message
            val warning = severity == MessageSeverity.WARNING || severity == MessageSeverity.ERROR
            hintMessageContainer.layer.apply {
                if (warning) {
                    borderColor = uiColorErgo.cgColor
                    borderWidth = 1.0
                    cornerRadius = 4.0
                } else {
                    borderWidth = 0.0
                    cornerRadius = 0.0
                }
            }
            hintMessageContainer.isHidden = false
        }
        if (tiToUse.hintMsg == null)
            hintMessageContainer.isHidden = true
    }
}