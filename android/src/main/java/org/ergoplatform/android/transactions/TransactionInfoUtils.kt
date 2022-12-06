package org.ergoplatform.android.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.EntryAddressTransactionBinding
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import org.ergoplatform.uilogic.transactions.getTransactionStateString
import org.ergoplatform.utils.millisecondsToLocalTime

fun inflateAddressTransactionEntry(
    layoutInflater: LayoutInflater,
    parent: ViewGroup,
    tx: AddressTransactionWithTokens,
    tokenClickListener: ((String) -> Unit)?
) = EntryAddressTransactionBinding.inflate(layoutInflater, parent, true).apply {
    bindData(layoutInflater, tx, tokenClickListener)
}

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

    txErgAmount.text = context.getString(
        R.string.label_erg_amount,
        txHeader.ergAmount.toStringTrimTrailingZeros()
    )
    txPurpose.text = txHeader.message
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