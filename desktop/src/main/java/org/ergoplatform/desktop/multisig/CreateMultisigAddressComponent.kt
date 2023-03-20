package org.ergoplatform.desktop.multisig

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pop
import com.arkivanov.decompose.router.popWhile
import com.arkivanov.decompose.router.push
import org.ergoplatform.Application
import org.ergoplatform.appkit.Address
import org.ergoplatform.compose.multisig.CreateMultisigAddressLayout
import org.ergoplatform.desktop.addressbook.AddressBookDialogStateHandler
import org.ergoplatform.desktop.ui.AppScrollingLayout
import org.ergoplatform.desktop.ui.navigation.NavClientScreenComponent
import org.ergoplatform.desktop.ui.navigation.NavHostComponent
import org.ergoplatform.desktop.ui.navigation.ScreenConfig
import org.ergoplatform.uilogic.multisig.CreateMultisigAddressUiLogic

class CreateMultisigAddressComponent(
    private val componentContext: ComponentContext,
    private val navHost: NavHostComponent
) : NavClientScreenComponent(navHost), ComponentContext by componentContext {

    override val appBarLabel: String
        get() = "" // no app bar shown

    override val fullScreen: Boolean
        get() = true

    private val uiLogic = CreateMultisigAddressUiLogic()
    private val participantsList = mutableStateListOf<Address>()

    private val addressBookDialogState = AddressBookDialogStateHandler()

    @Composable
    override fun renderScreenContents(scaffoldState: ScaffoldState?) {
        val participantAddress = rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }

        AppScrollingLayout {
            CreateMultisigAddressLayout(
                Modifier.align(Alignment.Center),
                Application.texts,
                uiLogic,
                onScanAddress = {
                    router.push(ScreenConfig.QrCodeScanner { qrCode ->
                        participantAddress.value =
                            TextFieldValue(uiLogic.getInputFromQrCode(qrCode))
                    })
                },
                onBack = navHost.router::pop,
                onProceed = {
                    router.popWhile { !(it is ScreenConfig.WalletList) }
                },
                getDb = { Application.database },
                participantAddress = participantAddress,
                onChooseRecipientAddress = { addressBookDialogState.showChooseAddressDialog() },
                participants = participantsList,
            )
        }

        addressBookDialogState.AddressBookDialogs(
            onChooseEntry = { addressWithLabel ->
                try {
                    uiLogic.addParticipantAddress(addressWithLabel.address, Application.texts)
                    participantsList.clear()
                    participantsList.addAll(uiLogic.participants)
                } catch (t: Throwable) {
                    participantAddress.value = TextFieldValue(addressWithLabel.address)
                }
            },
            componentScope()
        )
    }
}