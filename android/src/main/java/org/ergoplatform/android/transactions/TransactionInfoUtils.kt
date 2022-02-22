package org.ergoplatform.android.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.ergoplatform.ErgoAmount
import org.ergoplatform.TokenAmount
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.CardTransactionInfoBinding
import org.ergoplatform.android.databinding.EntryTransactionBoxBinding
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.transactions.TransactionInfo

fun CardTransactionInfoBinding.bindTransactionInfo(
    ti: TransactionInfo,
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
                layoutInflater
            )
        }
    }

    layoutOutboxes.apply {
        removeAllViews()

        ti.outputs.forEach { output ->
            bindBoxView(
                this, output.value, output.address, output.assets, layoutInflater
            )
        }
    }
}

private fun bindBoxView(
    container: ViewGroup,
    value: Long?,
    address: String,
    assets: List<AssetInstanceInfo>?,
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
        boxBinding.labelBoxAddress.maxLines =
            if (boxBinding.labelBoxAddress.maxLines == 1) 10 else 1
    }

    boxBinding.boxTokenEntries.apply {
        removeAllViews()
        visibility = View.GONE

        assets?.forEach {
            visibility = View.VISIBLE
            val tokenBinding =
                EntryWalletTokenBinding.inflate(layoutInflater, this, true)
            // we use the token id here, we don't have the name in the cold wallet context
            tokenBinding.labelTokenName.text = it.name ?: it.tokenId
            tokenBinding.labelTokenVal.text =
                TokenAmount(it.amount, it.decimals ?: 0).toStringUsFormatted()
        }
    }
}

