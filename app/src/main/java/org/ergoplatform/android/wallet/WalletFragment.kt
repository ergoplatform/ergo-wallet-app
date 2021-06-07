package org.ergoplatform.android.wallet

import StageConstants
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.ErgoFacade
import org.ergoplatform.android.NodeConnector
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.CardWalletBinding
import org.ergoplatform.android.databinding.FragmentWalletBinding
import java.io.InputStreamReader


class WalletFragment : Fragment() {

    private lateinit var walletViewModel: WalletViewModel

    private var _binding: FragmentWalletBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        walletViewModel =
            ViewModelProvider(this).get(WalletViewModel::class.java)
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val walletAdapter = WalletAdapter()
        binding.recyclerview.adapter = walletAdapter
        AppDatabase.getInstance(requireContext()).walletDao().getWalletsWithStates()
            .observe(viewLifecycleOwner,
                { walletList ->
                    walletAdapter.walletList = walletList

                    binding.swipeRefreshLayout.visibility =
                        if (walletList.isEmpty()) View.GONE else View.VISIBLE
                    binding.emptyView.root.visibility =
                        if (walletList.isEmpty()) View.VISIBLE else View.GONE
                })

        binding.emptyView.cardRestoreWallet.setOnClickListener {
            findNavController().navigate(R.id.restoreWalletFragmentDialog)
        }

        val nodeConnector = NodeConnector.getInstance()
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (!nodeConnector.refreshByUser(requireContext())) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        nodeConnector.isRefreshing.observe(viewLifecycleOwner, { isRefreshing ->
            binding.swipeRefreshLayout.isRefreshing = isRefreshing

            if (!isRefreshing) {
                refreshTimeSinceSyncLabel()
            }
        })
    }

    private fun refreshTimeSinceSyncLabel() {
        val nodeConnector = NodeConnector.getInstance()
        val lastRefresMs = nodeConnector.lastRefresMs
        binding.synctime.text = if (lastRefresMs > 0) getString(
            R.string.label_last_sync,
            if (System.currentTimeMillis() - lastRefresMs < 1000L) getString(R.string.label_last_sync_just_now) else
                DateUtils.getRelativeTimeSpanString(lastRefresMs)
        )
        else null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_wallet, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_wallet -> {
                findNavController().navigate(WalletFragmentDirections.actionToAddWalletChooserFragment())
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTimeSinceSyncLabel()
        NodeConnector.getInstance().refreshWhenNeeded(requireContext())
    }

    private fun runOnErgo() {
        val configPath = "config/freeze_coin_config.json"
        GlobalScope.launch(Dispatchers.Main) {
            val tx = withContext(Dispatchers.IO) {
                val inputStream = requireContext().assets.open(configPath)
                val configReader = InputStreamReader(inputStream)
                ErgoFacade.sendTx(1000000000, configReader)
            }
            System.out.println(tx)
            Snackbar.make(requireView(), "Tx: $tx", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }
}

class WalletAdapter : RecyclerView.Adapter<WalletViewHolder>() {
    var walletList: List<WalletDbEntity> = emptyList()
        set(value) {
            val diffCallback = WalletDiffCallback(field, value)
            field = value
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val cardWalletBinding =
            CardWalletBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val cardViewHolder = WalletViewHolder(cardWalletBinding)
        return cardViewHolder
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        holder.bind(walletList.get(position))
    }

    override fun getItemCount(): Int {
        return walletList.size
    }

}

class WalletViewHolder(val binding: CardWalletBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(wallet: WalletDbEntity) {
        binding.walletName.text = wallet.walletConfig.displayName
        binding.walletBalance.text = (wallet.state?.balance ?: 0).toString()
        binding.walletTransactions.text = (wallet.state?.transactions ?: 0).toString()

        val unconfirmed = (wallet.state?.unconfirmedBalance ?: 0) - (wallet.state?.balance ?: 0)
        binding.walletUnconfirmed.text = unconfirmed.toString()
        binding.walletUnconfirmed.visibility = if (unconfirmed == 0L) View.GONE else View.VISIBLE
        binding.labelWalletUnconfirmed.visibility = binding.walletUnconfirmed.visibility

        binding.buttonViewTransactions.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(StageConstants.EXPLORER_WEB_ADDRESS + "en/addresses/" + wallet.walletConfig.publicAddress)
            )
            binding.root.context.startActivity(browserIntent)
        }
    }

}

class WalletDiffCallback(val oldList: List<WalletDbEntity>, val newList: List<WalletDbEntity>) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList.get(oldItemPosition).walletConfig.id == newList.get(newItemPosition).walletConfig.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // always redraw
        return false
    }

}