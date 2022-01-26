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
open class TransactionContainer(private val texts: I18NBundle, private val clickListener: Runnable) : UIStackView() {
    private val inboxesList = UIStackView().apply {
        axis = UILayoutConstraintAxis.Vertical
    }
    private val outBoxesList = UIStackView().apply {
        axis = UILayoutConstraintAxis.Vertical
    }

    init {
        axis = UILayoutConstraintAxis.Vertical
        spacing = DEFAULT_MARGIN
        layoutMargins = UIEdgeInsets(DEFAULT_MARGIN * 2, 0.0, DEFAULT_MARGIN * 2, 0.0)
        isLayoutMarginsRelativeArrangement = true

        this.addArrangedSubview(Body2BoldLabel().apply {
            text = texts.get(STRING_DESC_SIGNING_REQUEST)
        })
        this.addArrangedSubview(createHorizontalSeparator())
        this.addArrangedSubview(Body1BoldLabel().apply {
            text = texts.get(STRING_TITLE_INBOXES)
            textColor = uiColorErgo
        })
        this.addArrangedSubview(Body2Label().apply {
            text = texts.get(STRING_DESC_INBOXES)
        })
        this.addArrangedSubview(inboxesList)
        this.addArrangedSubview(createHorizontalSeparator())
        this.addArrangedSubview(Body1BoldLabel().apply {
            text = texts.get(STRING_TITLE_OUTBOXES)
            textColor = uiColorErgo
        })
        this.addArrangedSubview(Body2Label().apply {
            text = texts.get(STRING_DESC_OUTBOXES)
        })
        this.addArrangedSubview(outBoxesList)

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

    fun bindTransaction(transactionInfo: TransactionInfo) {
        inboxesList.clearArrangedSubviews()
        transactionInfo.inputs.forEach { input ->
            inboxesList.addArrangedSubview(
                TransactionBoxEntryView().bindBoxView(
                    input.value,
                    input.address,
                    input.assets,
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
                    texts
                )
            )
        }
    }
}