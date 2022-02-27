package org.ergoplatform.android.tokens

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import org.ergoplatform.TokenAmount
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.android.databinding.EntryWalletTokenDetailsBinding
import org.ergoplatform.android.databinding.FragmentChooseTokenDialogItemBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.setTextAndVisibility
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.tokens.TokenEntryViewUiLogic

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
        TokenAmount(
            walletToken.amount ?: 0,
            walletToken.decimals,
        ).toStringPrettified()
}

class WalletDetailsTokenEntryView(val binding: EntryWalletTokenDetailsBinding, walletToken: WalletToken) :
    TokenEntryViewUiLogic(walletToken) {
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

    override fun setThumbnail(thumbnailType: Int) {
        val thumbnail = getThumbnailDrawableId(thumbnailType)
        binding.layoutThumbnail.visibility = if (thumbnail != 0) View.VISIBLE else View.GONE
        binding.imgThumbnail.setImageResource(thumbnail)
    }
}

class ChooseTokenEntryView(val binding: FragmentChooseTokenDialogItemBinding, walletToken: WalletToken) : TokenEntryViewUiLogic(walletToken) {
    override val texts: StringProvider
        get() = AndroidStringProvider(binding.root.context)

    override fun setDisplayedTokenName(tokenName: String) {
        binding.labelTokenName.text = tokenName
    }

    override fun setDisplayedTokenId(tokenId: String?) {
        binding.labelTokenId.setTextAndVisibility(tokenId)
    }

    override fun setDisplayedBalance(value: String?) {
        // not done here
    }

    override fun setDisplayedPrice(price: String?) {
        // not done here
    }

    override fun setGenuineFlag(genuineFlag: Int) {
        binding.labelTokenName.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0,
            0,
            getGenuineDrawableId(genuineFlag),
            0
        )
    }

    override fun setThumbnail(thumbnailType: Int) {
        val thumbnail = getThumbnailDrawableId(thumbnailType)
        binding.layoutThumbnail.visibility = if (thumbnail != 0) View.VISIBLE else View.GONE
        binding.imgThumbnail.setImageResource(thumbnail)
    }
}