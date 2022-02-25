package org.ergoplatform.android.tokens

import android.view.View
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.EntryWalletTokenDetailsBinding
import org.ergoplatform.persistance.TokenInformation
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.tokens.isSingularToken
import org.ergoplatform.utils.formatTokenAmounts

class WalletDetailsView(val binding: EntryWalletTokenDetailsBinding) {
    private var walletToken: WalletToken? = null
    private var tokenInformation: TokenInformation? = null
    val tokenId get() = walletToken?.tokenId

    fun bind(walletToken: WalletToken, tokenInformation: TokenInformation? = null) {
        this.walletToken = walletToken
        this.tokenInformation = tokenInformation

        binding.labelTokenName.text =
            walletToken.name ?: binding.root.context.getString(R.string.label_unnamed_token)
        binding.labelTokenId.text = walletToken.tokenId
        binding.labelTokenVal.text =
            formatTokenAmounts(
                walletToken.amount ?: 0,
                walletToken.decimals,
            )
        binding.labelTokenVal.visibility =
            if (walletToken.isSingularToken()) View.GONE else View.VISIBLE

        // TODO genuine: don't show ID
        // TODO show thumbnail (also in detail screen)
        // TODO show price
    }

    fun addTokenInfo(tokenInformation: TokenInformation) {
        if (this.tokenInformation == null || this.tokenInformation!!.updatedMs != tokenInformation.updatedMs) {
            bind(walletToken!!, tokenInformation)
        }
    }
}