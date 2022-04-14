package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentAddressTransactionsBinding
import org.ergoplatform.android.databinding.FragmentAddressTransactionsItemBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.android.ui.openUrlWithBrowser
import org.ergoplatform.android.wallet.addresses.AddressChooserCallback
import org.ergoplatform.android.wallet.addresses.ChooseAddressListDialogFragment
import org.ergoplatform.getExplorerAddressUrl
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.transactions.TransactionListManager
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens
import org.ergoplatform.wallet.addresses.getAddressLabel

class AddressTransactionsFragment : Fragment(), AddressChooserCallback {
    private var _binding: FragmentAddressTransactionsBinding? = null
    private val binding get() = _binding!!

    private val args: AddressTransactionsFragmentArgs by navArgs()
    private val viewModel: AddressTransactionViewModel by viewModels()
    private var adapter: TransactionsAdapter? = null
    private var adapterFinishedLoading = false

    private var wallet: Wallet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressTransactionsBinding.inflate(inflater, container, false)
        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.init(args.walletId, args.derivationIdx, AppDatabase.getInstance(requireContext()))

        viewModel.walletLiveData.observe(viewLifecycleOwner) {
            wallet = it
            refreshShownData()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    TransactionListManager.isDownloading.collect { isDownloading ->
                        if (isDownloading) binding.progressBar.show() else binding.progressBar.hide()
                        if (!isDownloading) {
                            binding.swipeRefreshLayout.isRefreshing = false
                            refreshShownData()
                        }
                    }
                }
                launch {
                    TransactionListManager.downloadProgress.collect { progress ->
                        val address = TransactionListManager.downloadAddress.value
                        if (progress > 0 && address == viewModel.derivedAddress?.publicAddress) {
                            binding.downloadProgress.visibility = View.VISIBLE
                            binding.downloadProgress.text =
                                getString(R.string.tx_download_progress, progress.toString())
                            refreshShownData()
                        } else {
                            binding.downloadProgress.visibility = View.GONE
                        }
                    }
                }
            }
        }

        // Click listener
        binding.addressLabel.setOnClickListener {
            wallet?.let { wallet ->
                ChooseAddressListDialogFragment.newInstance(
                    wallet.walletConfig.id,
                    false
                ).show(childFragmentManager, null)
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            startRefreshWhenNecessary()
            if (!TransactionListManager.isDownloading.value)
                binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun startRefreshWhenNecessary() {
        viewModel.derivedAddress?.let {
            val context = requireContext()
            TransactionListManager.downloadTransactionListForAddress(
                it.publicAddress,
                ApiServiceManager.getOrInit(Preferences(context)),
                AppDatabase.getInstance(context)
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_transactions, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_open) {

            openUrlWithBrowser(
                binding.root.context,
                getExplorerAddressUrl(viewModel.derivedAddress!!.publicAddress)
            )

            true
        } else
            super.onOptionsItemSelected(item)
    }

    override fun onAddressChosen(addressDerivationIdx: Int?) {
        viewModel.derivationIdx = addressDerivationIdx!!
        startRefreshWhenNecessary()
        refreshShownData(true)
    }

    private fun refreshShownData(forceReload: Boolean = false) {
        val wallet = viewModel.walletLiveData.value
        binding.fragmentTitle.text =
            getString(R.string.title_transactions) + " " + (wallet?.walletConfig?.displayName ?: "")
        binding.addressLabel.text =
            viewModel.derivedAddress?.getAddressLabel(AndroidStringProvider(requireContext()))

        if (forceReload || adapter == null || isRecyclerViewAtTop()) {
            // recreate adapter, no other way found for paging library to refresh completely
            val adapter = TransactionsAdapter()
            this.adapter = adapter

            binding.recyclerview.adapter = adapter
            adapter.addLoadStateListener { loadState ->
                adapterFinishedLoading = loadState.append.endOfPaginationReached
                val noItems = loadState.append.endOfPaginationReached && adapter.itemCount < 1
                binding.transactionsEmpty.visibility = if (noItems) View.VISIBLE else View.GONE

            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getDataFlow(AppDatabase.getInstance(requireContext()).transactionDbProvider)
                    .collectLatest {
                        adapter.submitData(it)
                    }
            }
        } else {
            adapter?.refresh()
        }
    }

    override fun onResume() {
        super.onResume()

        // reload, but only when at top to prevent refresh and scroll to top when user inspects
        // history
        if (isRecyclerViewAtTop()) {
            startRefreshWhenNecessary()
        }
    }

    private fun isRecyclerViewAtTop() =
        (binding.recyclerview.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() <= 0

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class TransactionsAdapter :
        PagingDataAdapter<AddressTransactionWithTokens, TransactionViewHolder>(TransactionDiffUtil()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TransactionViewHolder(
            FragmentAddressTransactionsItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
            val item = getItem(position)
            // Note that item may be null. ViewHolder must support binding a
            // null item as a placeholder.
            holder.bind(item, position == itemCount - 1 && adapterFinishedLoading)
        }
    }

    inner class TransactionViewHolder(private val binding: FragmentAddressTransactionsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AddressTransactionWithTokens?, isLast: Boolean) {
            item?.let {
                binding.entryAddress.bindData(
                    LayoutInflater.from(binding.root.context),
                    item
                ) { tokenId ->
                    findNavController().navigateSafe(
                        AddressTransactionsFragmentDirections.actionAddressTransactionsFragmentToTokenInformationDialogFragment(
                            tokenId
                        )
                    )
                }
                binding.buttonLoadAll.visibility = if (isLast) View.VISIBLE else View.GONE
                binding.descLoadAll.visibility = binding.buttonLoadAll.visibility
                if (isLast)
                    binding.buttonLoadAll.setOnClickListener {
                        val context = requireContext()
                        TransactionListManager.startDownloadAllAddressTransactions(
                            viewModel.derivedAddress!!.publicAddress,
                            ApiServiceManager.getOrInit(Preferences(context)),
                            AppDatabase.getInstance(context)
                        )
                    }
                binding.entryAddress.layoutTransactionInfo.setOnClickListener {
                    findNavController().navigateSafe(
                        AddressTransactionsFragmentDirections.actionAddressTransactionsFragmentToTransactionInfoFragment(
                            item.addressTransaction.txId
                        )
                    )
                }
            }
        }
    }

    class TransactionDiffUtil : DiffUtil.ItemCallback<AddressTransactionWithTokens>() {
        override fun areItemsTheSame(
            oldItem: AddressTransactionWithTokens,
            newItem: AddressTransactionWithTokens
        ) = oldItem.addressTransaction.txId == newItem.addressTransaction.txId

        override fun areContentsTheSame(
            oldItem: AddressTransactionWithTokens,
            newItem: AddressTransactionWithTokens
        ) = (oldItem.addressTransaction.state == newItem.addressTransaction.state)
        // every time the address transaction's contents change, a state change is also made -
        // so it is enough to check this field to know if the item changed

    }
}