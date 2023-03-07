package org.ergoplatform.android.multisig

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.zxing.integration.android.IntentIntegrator
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.ui.AbstractComposeFullScreenDialogFragment
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.QrScannerActivity
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.compose.multisig.CreateMultisigAddressLayout

class CreateMultisigAddressFragmentDialog : AbstractComposeFullScreenDialogFragment() {

    private val viewModel: CreateMultisigAddressViewModel by viewModels()

    @Composable
    override fun DialogContent() {
        val context = LocalContext.current

        Box(Modifier.fillMaxWidth()) {
            CreateMultisigAddressLayout(
                modifier = Modifier.align(Alignment.Center),
                texts = remember { AndroidStringProvider(context) },
                uiLogic = viewModel.uiLogic,
                onScanAddress = { QrScannerActivity.startFromFragment(this@CreateMultisigAddressFragmentDialog) },
                onBack = null,
                onProceed = {
                    NavHostFragment.findNavController(requireParentFragment())
                        .navigateSafe(CreateMultisigAddressFragmentDialogDirections.actionCreateMultisigAddressFragmentDialogToNavigationWallet())
                },
                getDb = { AppDatabase.getInstance(context) },
                participantAddress = viewModel.participantAddress,
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                viewModel.participantAddress.value =
                    TextFieldValue(viewModel.uiLogic.getInputFromQrCode(it))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}