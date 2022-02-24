package org.ergoplatform.android.tokens

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.android.databinding.EntryWalletTokenDetailsBinding
import org.ergoplatform.android.databinding.FragmentChooseTokenDialogItemBinding
import org.ergoplatform.persistance.GENUINE_SUSPICIOUS
import org.ergoplatform.persistance.GENUINE_VERIFIED
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.tokens.isSingularToken
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

    itemBinding.labelTokenName.text =
        walletToken.name ?: layoutInflater.context.getString(R.string.label_unnamed_token)
    itemBinding.labelTokenVal.text =
        formatTokenAmounts(
            walletToken.amount ?: 0,
            walletToken.decimals,
        )
}

fun inflateAndBindDetailedTokenEntryView(
    walletToken: WalletToken,
    linearLayout: LinearLayout,
    layoutInflater: LayoutInflater
): EntryWalletTokenDetailsBinding {
    val itemBinding =
        EntryWalletTokenDetailsBinding.inflate(
            layoutInflater,
            linearLayout,
            true
        )

    itemBinding.labelTokenName.text =
        walletToken.name ?: layoutInflater.context.getString(R.string.label_unnamed_token)
    itemBinding.labelTokenId.text = walletToken.tokenId
    itemBinding.labelTokenVal.text =
        formatTokenAmounts(
            walletToken.amount ?: 0,
            walletToken.decimals,
        )
    itemBinding.labelTokenVal.visibility =
        if (walletToken.isSingularToken()) View.GONE else View.VISIBLE

    return itemBinding
}

fun FragmentChooseTokenDialogItemBinding.bind(token: WalletToken) {
    labelTokenName.text = token.name ?: root.context.getString(R.string.label_unnamed_token)
    labelTokenId.text = token.tokenId
}

fun TokenInformation.getGenuineDrawableId(): Int {
    return when (genuineFlag) {
        GENUINE_VERIFIED -> R.drawable.ic_verified_24
        GENUINE_SUSPICIOUS -> R.drawable.ic_suspicious_24
        else -> 0
    }
}
