package org.ergoplatform.android.multisig

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.setText(R.string.title_multisigtx_details)
    }

    @Composable
    override fun FragmentContent() {
        val context = requireContext()
        MultisigTxDetailsLayout(
            Modifier,
            multisigTxDetails = viewModel.uiLogic.multisigTx.collectAsState().value,
            uiLogic = viewModel.uiLogic,
            texts = AndroidStringProvider(context),
            getDb = { AppDatabase.getInstance(context) }
        )
    }
}