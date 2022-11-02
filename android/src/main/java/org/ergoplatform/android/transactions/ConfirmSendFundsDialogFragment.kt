package org.ergoplatform.android.transactions

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.ui.AbstractComposeBottomSheetDialogFragment
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.desktop.transactions.SignTransactionInfoLayout

class ConfirmSendFundsDialogFragment : AbstractComposeBottomSheetDialogFragment() {

    @Composable
    override fun DialogContent() {
        val context = LocalContext.current
        val sendFundsFragment = parentFragment as SendFundsFragment

        SignTransactionInfoLayout(
            Modifier.padding(defaultPadding),
            sendFundsFragment.viewModel.preparedTxInfo!!,
            onConfirm = {
                dismiss()
                sendFundsFragment.startAuthFlow()
            },
            onTokenClick = null,
            AndroidStringProvider(context),
            getDb = { AppDatabase.getInstance(context) }
        )
    }
}