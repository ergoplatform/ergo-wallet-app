package org.ergoplatform.android.wallet.addresses

import android.content.Context
import android.view.View
import org.ergoplatform.android.ErgoAmount
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.IncludeWalletAddressInfoBinding
import org.ergoplatform.android.wallet.*

fun IncludeWalletAddressInfoBinding.fillAddressInformation(
    dbEntity: WalletAddressDbEntity,
    wallet: WalletDbEntity
) {
    val isDerivedAddress = dbEntity.isDerivedAddress()
    val ctx = root.context

    addressIndex.visibility =
        if (isDerivedAddress) View.VISIBLE else View.GONE
    addressIndex.text = dbEntity.derivationIndex.toString()
    addressLabel.text = dbEntity.getAddressLabel(ctx)
    publicAddress.text = dbEntity.publicAddress

    val state = wallet.getStateForAddress(dbEntity.publicAddress)
    val tokens = wallet.getTokensForAddress(dbEntity.publicAddress)
    addressBalance.amount = ErgoAmount(state?.balance ?: 0).toDouble()
    labelTokenNum.visibility =
        if (tokens.isNullOrEmpty()) View.GONE else View.VISIBLE
    labelTokenNum.text =
        ctx.getString(R.string.label_wallet_token_balance, tokens.size.toString())
}

fun WalletAddressDbEntity.getAddressLabel(ctx: Context): String {
    return (label ?: (if (isDerivedAddress()) ctx.getString(
        R.string.label_wallet_address_derived,
        derivationIndex.toString()
    ) else ctx.getString(R.string.label_wallet_main_address)))
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

fun WalletAddressDbEntity.isDerivedAddress(): Boolean {
    return derivationIndex > 0
}