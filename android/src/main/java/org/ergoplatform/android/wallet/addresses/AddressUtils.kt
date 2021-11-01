package org.ergoplatform.android.wallet.addresses

import android.view.View
import org.ergoplatform.ErgoAmount
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.IncludeWalletAddressInfoBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.wallet.*
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.wallet.addresses.getAddressLabel
import org.ergoplatform.wallet.addresses.isDerivedAddress

fun IncludeWalletAddressInfoBinding.fillAddressInformation(
    walletAddress: WalletAddress,
    wallet: WalletDbEntity
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
    addressBalance.amount = ErgoAmount(state?.balance ?: 0).toDouble()
    labelTokenNum.visibility =
        if (tokens.isNullOrEmpty()) View.GONE else View.VISIBLE
    labelTokenNum.text =
        ctx.getString(R.string.label_wallet_token_balance, tokens.size.toString())
}

fun IncludeWalletAddressInfoBinding.fillWalletAddressesInformation(
    wallet: WalletDbEntity
) {
    val ctx = root.context

    addressIndex.visibility = View.GONE
    addressLabel.text = ctx.getString(R.string.label_all_addresses, wallet.getNumOfAddresses())
    publicAddress.text = wallet.walletConfig.displayName

    val tokenNum = wallet.getTokensForAllAddresses().size
    labelTokenNum.visibility = if (tokenNum == 0) View.GONE else View.VISIBLE
    labelTokenNum.text =
        ctx.getString(R.string.label_wallet_token_balance, tokenNum.toString())

    addressBalance.amount =
        ErgoAmount(wallet.getBalanceForAllAddresses()).toDouble()
}