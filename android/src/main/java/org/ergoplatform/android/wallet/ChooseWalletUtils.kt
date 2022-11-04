package org.ergoplatform.android.wallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.ergoplatform.ErgoAmount
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentSendFundsWalletChooserItemBinding
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletConfig
import org.ergoplatform.wallet.getBalanceForAllAddresses
import org.ergoplatform.wallet.getTokensForAllAddresses
import org.ergoplatform.wallet.sortedByDisplayName

fun addWalletChooserItemBindings(
    layoutInflater: LayoutInflater,
    container: ViewGroup,
    wallets: List<Wallet>,
    showTokenNum: Boolean,
    clickListener: (WalletConfig) -> Unit
) {
    wallets.sortedByDisplayName().forEach { wallet ->
        val itemBinding = FragmentSendFundsWalletChooserItemBinding.inflate(
            layoutInflater, container, true
        )

        itemBinding.walletBalance.setAmount(
            ErgoAmount(wallet.getBalanceForAllAddresses()).toBigDecimal()
        )
        itemBinding.walletName.text = wallet.walletConfig.displayName
        val tokenNum = if (showTokenNum) wallet.getTokensForAllAddresses().size else 0
        itemBinding.labelTokenNum.visibility = if (tokenNum > 0) View.VISIBLE else View.GONE
        itemBinding.labelTokenNum.text = layoutInflater.context.getString(
            R.string.label_wallet_token_balance,
            tokenNum.toString()
        )

        itemBinding.root.setOnClickListener {
            clickListener.invoke(wallet.walletConfig)
        }
    }
}