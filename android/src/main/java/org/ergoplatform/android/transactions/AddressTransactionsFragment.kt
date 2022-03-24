package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.databinding.FragmentAddressTransactionsBinding
import org.ergoplatform.android.databinding.FragmentAddressTransactionsItemBinding
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.android.wallet.addresses.AddressChooserCallback
import org.ergoplatform.uilogic.transactions.AddressTransactionWithTokens

class AddressTransactionsFragment : Fragment(), AddressChooserCallback {
    private var _binding: FragmentAddressTransactionsBinding? = null
    private val binding get() = _binding!!

    private val args: AddressTransactionsFragmentArgs by navArgs()
    private val viewModel: AddressTransactionViewModel by viewModels()
    private val adapter = TransactionsAdapter()

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

        // TODO refresh, change address

        viewModel.walletLiveData.observe(viewLifecycleOwner) {
            refreshShownData()
        }
    }

    override fun onAddressChosen(addressDerivationIdx: Int?) {
        viewModel.derivationIdx = addressDerivationIdx!!
        refreshShownData()
    }

    private fun refreshShownData() {
        adapter.refresh()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getDataFlow(AppDatabase.getInstance(requireContext()).transactionDbProvider)
                .collectLatest {
                    adapter.submitData(it)
                }
        }
    }

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
            holder.bind(item)
        }
    }

    inner class TransactionViewHolder(private val binding: FragmentAddressTransactionsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AddressTransactionWithTokens?) {
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

    }
}