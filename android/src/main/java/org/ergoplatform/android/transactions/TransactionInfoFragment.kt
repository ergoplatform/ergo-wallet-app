package org.ergoplatform.android.transactions

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.addressbook.getAddressLabelFromDatabase
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentTransactionInfoBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.copyStringToClipboard
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.android.ui.shareText
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.transactions.TransactionInfo

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadTxInfo()

        viewModel.txInfo.observe(viewLifecycleOwner) { txInfo ->
            refreshScreenState(txInfo)

            txInfo?.let {
                binding.labelTransactionId.text = txInfo.id
                val purpose = viewModel.uiLogic.transactionPurpose
                binding.labelTransactionPurpose.text = purpose
                binding.labelTransactionPurpose.visibility =
                    if (purpose == null) View.GONE else View.VISIBLE
                binding.labelTransactionTimestamp.text =
                    viewModel.uiLogic.getTransactionExecutionState(
                        AndroidStringProvider(requireContext())
                    )

                bindTransactionInfo(
                    txInfo,
                    binding.layoutInboxes,
                    binding.layoutOutboxes,
                    tokenClickListener = { tokenId ->
                        findNavController().navigateSafe(
                            TransactionInfoFragmentDirections.actionTransactionInfoFragmentToTokenInformationDialogFragment(
                                tokenId
                            )
                        )
                    },
                    layoutInflater,
                    addressLabelHandler = { address, callback ->
                        context?.let { context ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                getAddressLabelFromDatabase(
                                    AppDatabase.getInstance(context),
                                    address,
                                    AndroidStringProvider(context)
                                )?.let { callback(it) }
                            }
                        }
                    }
                )

                binding.layoutTxinfo.layoutTransition = LayoutTransition()
                binding.layoutTxinfo.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            }
        }

        binding.labelTransactionId.setOnClickListener {
            copyStringToClipboard(binding.labelTransactionId.text.toString(), requireContext(), it)
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