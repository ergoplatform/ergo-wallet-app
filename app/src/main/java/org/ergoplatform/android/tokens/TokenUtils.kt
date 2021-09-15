package org.ergoplatform.android.tokens

import android.view.LayoutInflater
import android.widget.LinearLayout
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.android.ui.formatTokenAmounts
import org.ergoplatform.android.wallet.WalletTokenDbEntity

fun inflateAndBindTokenView(
    walletTokenDbEntity: WalletTokenDbEntity,
    linearLayout: LinearLayout,
    layoutInflater: LayoutInflater
) {
    val itemBinding =
        EntryWalletTokenBinding.inflate(
            layoutInflater,
            linearLayout,
            true
        )

    itemBinding.labelTokenName.text = walletTokenDbEntity.name
    itemBinding.labelTokenVal.text =
        formatTokenAmounts(
            walletTokenDbEntity.amount ?: 0,
            walletTokenDbEntity.decimals ?: 0,
        )
}