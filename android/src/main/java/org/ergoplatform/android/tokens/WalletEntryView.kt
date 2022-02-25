package org.ergoplatform.android.tokens

import org.ergoplatform.android.databinding.EntryWalletTokenDetailsBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.setTextAndVisibility
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.tokens.WalletEntryViewUiLogic

class WalletEntryView(val binding: EntryWalletTokenDetailsBinding, walletToken: WalletToken) :
    WalletEntryViewUiLogic(walletToken) {
    override val texts: StringProvider
        get() = AndroidStringProvider(binding.root.context)

    override fun setDisplayedTokenName(tokenName: String) {
        binding.labelTokenName.text = tokenName
    }

    override fun setGenuineFlag(genuineFlag: Int) {
        binding.labelTokenName.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            getGenuineDrawableId(genuineFlag),
            0
        )
    }

    override fun setDisplayedTokenId(tokenId: String?) {
        binding.labelTokenId.setTextAndVisibility(tokenId)
    }

    override fun setDisplayedBalance(value: String?) {
        binding.labelTokenVal.setTextAndVisibility(value)
    }

    override fun setDisplayedPrice(price: String?) {
        binding.labelBalanceValue.setTextAndVisibility(price)
    }

}