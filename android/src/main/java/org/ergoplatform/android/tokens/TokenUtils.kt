package org.ergoplatform.android.tokens

import android.view.LayoutInflater
import android.widget.LinearLayout
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.utils.formatTokenAmounts

fun inflateAndBindTokenView(
    walletToken: WalletToken,
    linearLayout: LinearLayout,
    layoutInflater: LayoutInflater
) {
    val itemBinding =
        EntryWalletTokenBinding.inflate(
            layoutInflater,
            linearLayout,
            true
        )

    itemBinding.labelTokenName.text = walletToken.name
    itemBinding.labelTokenVal.text =
        formatTokenAmounts(
            walletToken.amount ?: 0,
            walletToken.decimals ?: 0,
        )
}