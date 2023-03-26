package org.ergoplatform.android.multisig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.ui.AbstractComposeFragment
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.compose.multisig.MultisigTxDetailsLayout

class MultisigTxDetailsFragment : AbstractComposeFragment() {

    val viewModel: MultisigTxDetailViewModel by viewModels()
    private val args: MultisigTxDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.uiLogic.initMultisigTx(args.dbId, AppDatabase.getInstance(requireContext()))
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @Composable
    override fun FragmentContent() {
        Box {
            MultisigTxDetailsLayout(
                Modifier.align(Alignment.Center),
                multisigTxDetails = viewModel.uiLogic.multisigTx.collectAsState().value,
                uiLogic = viewModel.uiLogic,
                texts = AndroidStringProvider(requireContext()),
            )
        }
    }
}