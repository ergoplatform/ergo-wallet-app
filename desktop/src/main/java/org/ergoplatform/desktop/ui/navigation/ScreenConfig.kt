package org.ergoplatform.desktop.ui.navigation

import com.arkivanov.essenty.parcelable.Parcelable
import org.ergoplatform.appkit.SecretString
import org.ergoplatform.persistance.WalletConfig

sealed class ScreenConfig : Parcelable {
    object WalletList : ScreenConfig()
    object Settings : ScreenConfig()
    object AddWalletChooser : ScreenConfig()
    object AddReadOnlyWallet : ScreenConfig()
    object CreateWallet : ScreenConfig()
    data class ConfirmCreateWallet(val mnemonic: SecretString) : ScreenConfig()
    object RestoreWallet : ScreenConfig()
    data class SaveWallet(val mnemonic: SecretString, val fromRestore: Boolean) : ScreenConfig()
    data class SendFunds(val walletConfig: WalletConfig) : ScreenConfig()
    data class ReceiveToWallet(val walletConfig: WalletConfig) : ScreenConfig()
    data class WalletConfiguration(val walletConfig: WalletConfig) : ScreenConfig()
    data class WalletAddressesList(val walletConfig: WalletConfig) : ScreenConfig()
    data class QrCodeScanner(val callback: (String) -> Unit) : ScreenConfig()
}