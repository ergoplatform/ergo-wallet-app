package org.ergoplatform.android.transactions

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentTransactionInfoBinding
import org.ergoplatform.android.ui.*
import org.ergoplatform.compose.settings.AppButton
import org.ergoplatform.compose.settings.defaultPadding
import org.ergoplatform.compose.transactions.TransactionInfoLayout
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.transactions.ErgoPay
import org.ergoplatform.transactions.TransactionInfo
import org.ergoplatform.uilogic.STRING_BUTTON_CANCEL_TX

/**
 * Shows all transaction information by fetching it from Explorer by its transactionId
 */
class TransactionInfoFragment : Fragment() {
    private var _binding: FragmentTransactionInfoBinding? = null
    private val binding get() = _binding!!

    private val args: TransactionInfoFragmentArgs by navArgs()
    private val viewModel: TransactionInfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun onCancelTx() {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setMessage(R.string.info_cancel_tx)
            .setPositiveButton(
                R.string.zxing_button_ok
            ) { _, _ ->
                viewModel.doCancelTx(
                    AppDatabase.getInstance(context).walletDbProvider,
                    Preferences(context),
                    AndroidStringProvider(context),
                )
            }
            .show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadTxInfo()

        binding.tiComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                AppComposeTheme {
                    val txInfoState = viewModel.txInfo.observeAsState()
                    val canCancel = viewModel.canCancel.observeAsState(false)
                    txInfoState.value?.let { txInfo ->
                        val context = requireContext()
                        Column {
                            val texts = AndroidStringProvider(context)
                            TransactionInfoLayout(
                                modifier = Modifier.padding(defaultPadding),
                                uiLogic = viewModel.uiLogic,
                                txInfo,
                                onTxIdClicked = {
                                    copyStringToClipboard(
                                        txInfo.id,
                                        requireContext(),
                                        this@apply
                                    )
                                },
                                onTokenClick = { tokenId ->
                                    findNavController().navigateSafe(
                                        TransactionInfoFragmentDirections.actionTransactionInfoFragmentToTokenInformationDialogFragment(
                                            tokenId
                                        )
                                    )
                                },
                                texts = texts,
                                getDb = { AppDatabase.getInstance(context) }
                            )

                            if (canCancel.value) {
                                AppButton(
                                    ::onCancelTx,
                                    Modifier
                                        .padding(defaultPadding)
                                        .align(Alignment.CenterHorizontally),
                                ) {
                                    Text(texts.getString(STRING_BUTTON_CANCEL_TX))
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.txInfo.observe(viewLifecycleOwner) { txInfo ->
            refreshScreenState(txInfo)
            txInfo?.let {
                binding.layoutTxinfo.layoutTransition = LayoutTransition()
                binding.layoutTxinfo.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            }
        }

        viewModel.cancelTxPromptSigning.observe(viewLifecycleOwner) { prompt ->
            if (prompt == null)
                return@observe

            if (!prompt.success)
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(prompt.errorMsg!!)
                    .setPositiveButton(R.string.zxing_button_ok, null)
                    .show()
            else {
                val walletConfigAndDerivedIdx = viewModel.walletConfigAndDerivedIdx
                findNavController().navigateSafe(
                    TransactionInfoFragmentDirections.actionTransactionInfoFragmentToErgoPaySigningFragment(
                        ErgoPay.buildStaticSigningRequest(prompt.serializedTx!!),
                        walletConfigAndDerivedIdx?.first ?: -1,
                        walletConfigAndDerivedIdx?.second ?: -1,
                    )
                )
            }
        }
    }

    private fun refreshScreenState(txInfo: TransactionInfo?) {
        val isLoading = viewModel.uiLogic.isLoading
        val isError = txInfo == null && !isLoading

        binding.progressCircular.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvError.visibility = if (isError) View.VISIBLE else View.GONE
        binding.layoutTxinfo.visibility =
            if (isLoading || txInfo == null) View.GONE else View.VISIBLE

        activity?.invalidateOptionsMenu()
    }

    private fun loadTxInfo(forceReload: Boolean = false) {
        val context = requireContext()
        viewModel.uiLogic.init(
            args.txId,
            args.address,
            ApiServiceManager.getOrInit(Preferences(context)),
            AppDatabase.getInstance(context),
            forceReload
        )
        refreshScreenState(viewModel.txInfo.value)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_transaction_info, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_reload).isVisible = viewModel.uiLogic.shouldOfferReloadButton()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_share) {
            viewModel.uiLogic.txId?.let {
                shareText(getExplorerTxUrl(it))
            }
            true
        } else if (item.itemId == R.id.menu_reload) {
            loadTxInfo(forceReload = true)
            true
        } else
            super.onOptionsItemSelected(item)
    }
}