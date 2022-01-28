package org.ergoplatform.android.wallet.addresses

import android.view.View
import org.ergoplatform.ErgoAmount
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.IncludeWalletAddressInfoBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.wallet.*
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.addresses.isDerivedAddress

fun IncludeWalletAddressInfoBinding.fillAddressInformation(
    walletAddress: WalletAddress,
    wallet: Wallet
) {
    val isDerivedAddress = walletAddress.isDerivedAddress()
    val ctx = root.context

    addressIndex.visibility =
        if (isDerivedAddress) View.VISIBLE else View.GONE
    addressIndex.text = walletAddress.derivationIndex.toString()
    addressLabel.text = walletAddress.getAddressLabel(AndroidStringProvider(ctx))
    publicAddress.text = walletAddress.publicAddress

    val state = wallet.getStateForAddress(walletAddress.publicAddress)
    val tokens = wallet.getTokensForAddress(walletAddress.publicAddress)
    addressBalance.setAmount(ErgoAmount(state?.balance ?: 0).toBigDecimal())
    labelTokenNum.visibility =
        if (tokens.isNullOrEmpty()) View.GONE else View.VISIBLE
    labelTokenNum.text =
        ctx.getString(R.string.label_wallet_token_balance, tokens.size.toString())
}

fun IncludeWalletAddressInfoBinding.fillWalletAddressesInformation(
    wallet: Wallet
) {
    val ctx = root.context

    addressIndex.visibility = View.GONE
    addressLabel.text = ctx.getString(R.string.label_all_addresses, wallet.getNumOfAddresses())
    publicAddress.text = wallet.walletConfig.displayName

    val tokenNum = wallet.getTokensForAllAddresses().size
    labelTokenNum.visibility = if (tokenNum == 0) View.GONE else View.VISIBLE
    labelTokenNum.text =
        ctx.getString(R.string.label_wallet_token_balance, tokenNum.toString())

    addressBalance.setAmount(
        ErgoAmount(wallet.getBalanceForAllAddresses()).toBigDecimal()
    )
}