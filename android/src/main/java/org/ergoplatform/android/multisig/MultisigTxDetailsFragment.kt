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
import org.ergoplatform.SigningSecrets
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.ui.AbstractAuthenticationFragment
import org.ergoplatform.android.ui.AbstractComposeFragment
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.compose.multisig.MultisigTxDetailsLayout
import org.ergoplatform.persistance.WalletConfig

class MultisigTxDetailsFragment : AbstractAuthenticationFragment() {

    val viewModel: MultisigTxDetailViewModel by viewModels()
    private val args: MultisigTxDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.uiLogic.initMultisigTx(args.dbId, AppDatabase.getInstance(requireContext()))
        val binding = AbstractComposeFragment.inflateComposeViewBinding(inflater, container) {
            FragmentContent()
        }
        binding.tvTitle.setText(R.string.title_multisigtx_details)
        return binding.root
    }

    override var authenticationWalletConfig: WalletConfig? = null

    @Composable
    private fun FragmentContent() {
        val context = requireContext()
        MultisigTxDetailsLayout(
            Modifier,
            multisigTxDetails = viewModel.uiLogic.multisigTx.collectAsState().value,
            uiLogic = viewModel.uiLogic,
            onSignWith = {
                authenticationWalletConfig = it
                startAuthFlow()
            },
            texts = AndroidStringProvider(context),
            getDb = { AppDatabase.getInstance(context) }
        )
    }

    override fun proceedFromAuthFlow(secrets: SigningSecrets) {
        authenticationWalletConfig?.let {
            viewModel.uiLogic.signWith(it, secrets)
        }
        authenticationWalletConfig = null
    }
}