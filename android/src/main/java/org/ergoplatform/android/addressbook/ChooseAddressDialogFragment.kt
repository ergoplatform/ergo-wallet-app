package org.ergoplatform.android.addressbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.ui.AbstractComposeBottomSheetDialogFragment
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.compose.addressbook.ChooseAddressLayout
import org.ergoplatform.persistance.IAddressWithLabel

class ChooseAddressDialogFragment : AbstractComposeBottomSheetDialogFragment() {
    @Composable
    override fun DialogContent() {
        val context = LocalContext.current

        val addressesList =
            AppDatabase.getInstance(context).addressBookDbProvider.getAllAddressEntries()
                .collectAsState(emptyList())

        val walletsWithStates =
            AppDatabase.getInstance(context).walletDao().getWalletsWithStates().observeAsState(
                emptyList()
            )

        ChooseAddressLayout(
            walletsWithStates.value.map { it.toModel() },
            addressesList.value,
            onChooseEntry = {
                (parentFragment as? ChooseAddressDialogCallback)?.onAddressChosen(it)
                dismiss()
            },
            onEditEntry = {
                EditAddressDialogFragment.newInstance(it?.id ?: 0).show(parentFragmentManager, null)
            },
            stringProvider = remember { AndroidStringProvider(context) }
        )
    }
}

interface ChooseAddressDialogCallback {
    fun onAddressChosen(address: IAddressWithLabel)
}