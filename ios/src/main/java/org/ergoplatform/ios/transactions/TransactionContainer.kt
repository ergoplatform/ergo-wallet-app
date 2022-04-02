package org.ergoplatform.ios.transactions

import com.badlogic.gdx.utils.I18NBundle
import org.ergoplatform.ios.ui.*
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.*
import org.robovm.apple.coregraphics.CGRect
import org.robovm.apple.uikit.UIEdgeInsets
import org.robovm.apple.uikit.UILayoutConstraintAxis
import org.robovm.apple.uikit.UIStackView
import org.robovm.apple.uikit.UIView

/**
 * Shows transaction info with boxes and tokens to spend and boxes and tokens to issue
 */
abstract class TransactionContainer(private val texts: I18NBundle) : UIStackView() {
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

    open fun bindTransaction(transactionInfo: TransactionInfo, tokenClickListener: ((String) -> Unit)?) {
        inboxesList.clearArrangedSubviews()
        transactionInfo.inputs.forEach { input ->
            inboxesList.addArrangedSubview(
                TransactionBoxEntryView().bindBoxView(
                    input.value,
                    input.address,
                    input.assets,
                    tokenClickListener,
                    texts
                )
            )
        }
        outBoxesList.clearArrangedSubviews()
        transactionInfo.outputs.forEach { output ->
            outBoxesList.addArrangedSubview(
                TransactionBoxEntryView().bindBoxView(
                    output.value,
                    output.address,
                    output.assets,
                    tokenClickListener,
                    texts
                )
            )
        }
    }
}

open class SigningTransactionContainer(
    private val texts: I18NBundle,
    private val clickListener: Runnable
) : TransactionContainer(texts) {

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
                clickListener.run()
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