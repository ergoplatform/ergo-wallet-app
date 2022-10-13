package org.ergoplatform.android.addressbook

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.viewModels
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.ui.AbstractComposeBottomSheetDialogFragment
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.compose.addressbook.EditAddressEntryLayout

class EditAddressDialogFragment : AbstractComposeBottomSheetDialogFragment() {
    private val viewModel: EditAddressDialogViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.uiLogic.init(
            requireArguments().getInt(ARG_ADDRESS_ID),
            AppDatabase.getInstance(requireContext()).addressBookDbProvider
        )
    }

    @Composable
    override fun DialogContent() {
        val context = LocalContext.current

        EditAddressEntryLayout(
            viewModel.addressLiveData.observeAsState().value!!,
            stringProvider = remember { AndroidStringProvider(context) },
            viewModel.uiLogic,
            onSaved = { dismiss() }
        )
    }

    companion object {
        private const val ARG_ADDRESS_ID = "ARG_ADDRESS_ID"

        fun newInstance(addressId: Int): EditAddressDialogFragment {
            val addressEditDialog = EditAddressDialogFragment()
            val args = Bundle()
            args.putInt(ARG_ADDRESS_ID, addressId)
            addressEditDialog.arguments = args
            return addressEditDialog
        }
    }
}

