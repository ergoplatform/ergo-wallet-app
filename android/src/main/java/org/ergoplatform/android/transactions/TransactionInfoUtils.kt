package org.ergoplatform.android.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.TransitionManager
import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.CardTransactionInfoBinding
import org.ergoplatform.android.databinding.EntryAddressTransactionBinding
import org.ergoplatform.android.databinding.EntryTransactionBoxBinding
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import org.ergoplatform.uilogic.transactions.getTransactionStateString
import org.ergoplatform.utils.millisecondsToLocalTime

fun CardTransactionInfoBinding.bindTransactionInfo(
    ti: TransactionInfo,
    tokenClickListener: ((String) -> Unit)?,
    layoutInflater: LayoutInflater
) {
    layoutInboxes.apply {
        removeAllViews()

        ti.inputs.forEach { input ->
            bindBoxView(
                this,
                input.value,
                input.address ?: input.boxId,
                input.assets,
                tokenClickListener,
                layoutInflater
            )
        }
    }

    layoutOutboxes.apply {
        removeAllViews()

        ti.outputs.forEach { output ->
            bindBoxView(
                this,
                output.value,
                output.address,
                output.assets,
                tokenClickListener,
                layoutInflater
            )
        }
    }
}

private fun bindBoxView(
    container: ViewGroup,
    value: Long?,
    address: String,
    assets: List<AssetInstanceInfo>?,
    tokenClickListener: ((String) -> Unit)?,
    layoutInflater: LayoutInflater
) {
    val boxBinding = EntryTransactionBoxBinding.inflate(layoutInflater, container, true)
    boxBinding.boxErgAmount.text = container.context.getString(
        R.string.label_erg_amount,
        ErgoAmount(value ?: 0).toStringTrimTrailingZeros()
    )
    boxBinding.boxErgAmount.visibility =
        if (value == null || value == 0L) View.GONE else View.VISIBLE
    boxBinding.labelBoxAddress.text = address
    boxBinding.labelBoxAddress.setOnClickListener {
        TransitionManager.beginDelayedTransition(container)
        boxBinding.labelBoxAddress.maxLines =
            if (boxBinding.labelBoxAddress.maxLines == 1) 10 else 1
    }

    boxBinding.boxTokenEntries.apply {
        removeAllViews()
        visibility = View.GONE

        assets?.forEach { token ->
            visibility = View.VISIBLE
            val tokenBinding =
                EntryWalletTokenBinding.inflate(layoutInflater, this, true)
            // we use the token id here, we don't have the name in the cold wallet context
            tokenBinding.labelTokenName.text = token.name ?: token.tokenId
            tokenBinding.labelTokenVal.text =
                TokenAmount(token.amount, token.decimals ?: 0).toStringUsFormatted()
            tokenClickListener?.let {
                tokenBinding.root.setOnClickListener { tokenClickListener(token.tokenId) }
            }
        }
    }
}

fun inflateAddressTransactionEntry(
    layoutInflater: LayoutInflater,
    parent: ViewGroup,
    tx: AddressTransactionWithTokens,
    tokenClickListener: ((String) -> Unit)?
) = EntryAddressTransactionBinding.inflate(layoutInflater, parent, true)
    .bindData(layoutInflater, tx, tokenClickListener)

fun EntryAddressTransactionBinding.bindData(
    layoutInflater: LayoutInflater,
    tx: AddressTransactionWithTokens,
    tokenClickListener: ((String) -> Unit)?
) {
    val context = layoutInflater.context
    val stringProvider = AndroidStringProvider(context)
    val txHeader = tx.addressTransaction
    val txTokens = tx.tokens


    labelTransactionDate.text =
        if (txHeader.timestamp > 0) {
            millisecondsToLocalTime(txHeader.timestamp)
        } else {
            ""
        }

    boxErgAmount.text = context.getString(
        R.string.label_erg_amount,
        txHeader.ergAmount.toStringTrimTrailingZeros()
    )

    labelTransactionState.text = txHeader.getTransactionStateString(stringProvider)

    transactionTokenEntries.apply {
        removeAllViews()
        visibility = View.GONE

        txTokens.forEach { token ->
            visibility = View.VISIBLE
            val tokenBinding =
                EntryWalletTokenBinding.inflate(layoutInflater, this, true)
            // we use the token id here, we don't have the name in the cold wallet context
            tokenBinding.labelTokenName.text = token.name.ifBlank { token.tokenId }
            tokenBinding.labelTokenVal.text = token.tokenAmount.toStringUsFormatted()
            tokenClickListener?.let {
                tokenBinding.root.setOnClickListener { tokenClickListener(token.tokenId) }
            }
        }
    }
}