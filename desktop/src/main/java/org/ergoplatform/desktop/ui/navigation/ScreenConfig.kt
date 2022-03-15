package org.ergoplatform.desktop.ui.navigation

import com.arkivanov.essenty.parcelable.Parcelable

sealed class ScreenConfig : Parcelable {
    object WalletList : ScreenConfig()
    object Settings : ScreenConfig()
    data class SendFunds(val name: String) : ScreenConfig()
}