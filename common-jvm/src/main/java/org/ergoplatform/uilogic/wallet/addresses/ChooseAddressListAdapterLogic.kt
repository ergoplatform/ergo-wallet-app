package org.ergoplatform.uilogic.wallet.addresses

import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.wallet.getSortedDerivedAddressesList

class ChooseAddressListAdapterLogic(val wallet: Wallet, showAllAddresses: Boolean) {
    val addresses = wallet.getSortedDerivedAddressesList()
    val addAllAddresses = showAllAddresses && addresses.size > 1

    val itemCount get() = if (addAllAddresses) addresses.size + 1 else addresses.size

    fun bindViewHolder(holder: AddressHolder, position: Int) {
        if (!addAllAddresses) {
            val address = addresses.get(position)
            holder.bindAddress(address, wallet)
        } else if (position > 0) {
            val address = addresses.get(position - 1)
            holder.bindAddress(address, wallet)
        } else {
            holder.bindAllAddresses(wallet)
        }
    }

    interface AddressHolder {
        fun bindAddress(address: WalletAddress, wallet: Wallet)
        fun bindAllAddresses(wallet: Wallet)
    }
}