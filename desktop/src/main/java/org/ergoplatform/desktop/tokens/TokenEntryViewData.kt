package org.ergoplatform.desktop.tokens

import org.ergoplatform.Application
import org.ergoplatform.persistance.WalletToken
import org.ergoplatform.uilogic.StringProvider
import org.ergoplatform.uilogic.tokens.TokenEntryViewUiLogic

class TokenEntryViewData(walletToken: WalletToken, noIdWhenPriceAvail: Boolean) :
    TokenEntryViewUiLogic(walletToken, noIdWhenPriceAvail) {
    override val texts: StringProvider get() = Application.texts

    var displayedName: String? = null
    var displayedId: String? = null
    var balance: String? = null
    var price: String? = null
    var genuityFlag: Int? = null

    override fun setDisplayedTokenName(tokenName: String) {
        displayedName = tokenName
    }

    override fun setDisplayedTokenId(tokenId: String?) {
        displayedId = tokenId
    }

    override fun setDisplayedBalance(value: String?) {
        balance = value
    }

    override fun setDisplayedPrice(price: String?) {
        this.price = price
    }

    override fun setGenuineFlag(genuineFlag: Int) {
        genuityFlag = genuineFlag
    }

    override fun setThumbnail(thumbnailType: Int) {

    }
}