package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.ergoplatform.ErgoAmount
import org.ergoplatform.NodeConnector
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.android.databinding.CardWalletBinding
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.android.databinding.FragmentWalletBinding
import org.ergoplatform.android.tokens.inflateAndBindTokenView
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.utils.getTimeSpanString
import org.ergoplatform.wallet.getBalanceForAllAddresses
import org.ergoplatform.wallet.getTokensForAllAddresses
import org.ergoplatform.wallet.getUnconfirmedBalanceForAllAddresses
import java.util.*


class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null

    // save last shown wallet list in case view is destroyed
    // this is to preserve user's scroll position
    private var lastWalletList: List<Wallet> = emptyList()

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
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val walletAdapter = WalletAdapter(lastWalletList)
        binding.recyclerview.adapter = walletAdapter
        AppDatabase.getInstance(requireContext()).walletDao().getWalletsWithStates()
            .observe(viewLifecycleOwner,
                {
                    val walletList = it.map { it.toModel() }
                    walletAdapter.walletList = walletList.sortedBy {
                        it.walletConfig.displayName?.lowercase(
                            Locale.getDefault()
                        )
                    }
                    lastWalletList = walletAdapter.walletList

                    binding.swipeRefreshLayout.visibility =
                        if (walletList.isEmpty()) View.GONE else View.VISIBLE
                    binding.emptyView.root.visibility =
                        if (walletList.isEmpty()) View.VISIBLE else View.GONE
                })

        binding.emptyView.cardRestoreWallet.setOnClickListener {
            findNavController().navigate(R.id.restoreWalletFragmentDialog)
        }
        binding.emptyView.cardReadonlyWallet.setOnClickListener {
            NavHostFragment.findNavController(requireParentFragment())
                .navigate(R.id.addReadOnlyWalletFragmentDialog)
        }

        binding.emptyView.cardCreateWallet.setOnClickListener {
            NavHostFragment.findNavController(requireParentFragment())
                .navigate(R.id.createWalletDialog)
        }


        val nodeConnector = NodeConnector.getInstance()
        val rotateAnimation =
            AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_indefinitely)
        rotateAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {

            }

            override fun onAnimationRepeat(animation: Animation?) {
                if (!nodeConnector.isRefreshing.value) {
                    binding.ergoLogoBack.clearAnimation()
                }
            }

        })
        binding.swipeRefreshLayout.setOnRefreshListener {
            val context = requireContext()
            if (!nodeConnector.refreshByUser(
                    Preferences(context),
                    RoomWalletDbProvider(AppDatabase.getInstance(context))
                )
            ) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    nodeConnector.isRefreshing.collect { isRefreshing ->
                        if (!isRefreshing) {
                            binding.swipeRefreshLayout.isRefreshing = false
                            binding.connectionError.visibility =
                                if (nodeConnector.lastHadError) View.VISIBLE else View.INVISIBLE
                            refreshTimeSinceSyncLabel()
                        } else {
                            binding.ergoLogoBack.clearAnimation()
                            binding.ergoLogoBack.startAnimation(rotateAnimation)
                        }
                    }
                }
                launch {
                    nodeConnector.fiatValue.collect { value ->
                        if (value == 0f) {
                            binding.ergoPrice.visibility = View.GONE
                        } else {
                            binding.ergoPrice.visibility = View.VISIBLE
                            binding.ergoPrice.amount = value.toDouble()
                            binding.ergoPrice.setSymbol(
                                nodeConnector.fiatCurrency.toUpperCase(
                                    Locale.getDefault()
                                )
                            )
                        }
                        binding.labelErgoPrice.visibility = binding.ergoPrice.visibility
                    }
                }
            }
        }
    }

    private fun refreshTimeSinceSyncLabel() {
        val nodeConnector = NodeConnector.getInstance()
        val lastRefresMs = nodeConnector.lastRefreshMs
        binding.synctime.text = if (lastRefresMs > 0) getString(
            R.string.label_last_sync,
            getTimeSpanString(
                (System.currentTimeMillis() - lastRefresMs) / 1000L,
                AndroidStringProvider(requireContext())
            )
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
                findNavController().navigateSafe(WalletFragmentDirections.actionToAddWalletChooserFragment())
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
        val context = requireContext()
        NodeConnector.getInstance().refreshWhenNeeded(
            Preferences(context),
            RoomWalletDbProvider(AppDatabase.getInstance(context))
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class WalletAdapter(initWalletList: List<Wallet>) :
    RecyclerView.Adapter<WalletViewHolder>() {
    var walletList: List<Wallet> = initWalletList
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
    fun bind(wallet: Wallet) {
        binding.walletName.text = wallet.walletConfig.displayName
        binding.walletBalance.amount = ErgoAmount(wallet.getBalanceForAllAddresses()).toDouble()

        // Fill token headline
        val tokens = wallet.getTokensForAllAddresses()
        val tokenCount = tokens.size
        binding.walletTokenNum.text = tokenCount.toString()
        binding.walletTokenNum.visibility = if (tokenCount == 0) View.GONE else View.VISIBLE
        binding.labelTokenNum.visibility = binding.walletTokenNum.visibility
        binding.walletTokenUnfold.visibility = binding.walletTokenNum.visibility
        binding.walletTokenEntries.visibility =
            if (tokenCount == 0 || !wallet.walletConfig.unfoldTokens) View.GONE else View.VISIBLE
        binding.walletTokenUnfold.setImageResource(
            if (wallet.walletConfig.unfoldTokens)
                R.drawable.ic_remove_circle_24 else R.drawable.ic_add_circle_24
        )

        // Fill unconfirmed fields
        val unconfirmed = wallet.getUnconfirmedBalanceForAllAddresses()
        binding.walletUnconfirmed.amount = ErgoAmount(unconfirmed).toDouble()
        binding.walletUnconfirmed.visibility = if (unconfirmed == 0L) View.GONE else View.VISIBLE
        binding.labelWalletUnconfirmed.visibility = binding.walletUnconfirmed.visibility

        // Set button listeners
        val navigateToDetailsView: (v: View) -> Unit = {
            NavHostFragment.findNavController(itemView.findFragment())
                .navigateSafe(
                    WalletFragmentDirections.actionNavigationWalletToNavigationWalletDetails(
                        wallet.walletConfig.id
                    )
                )
        }
        binding.cardView.setOnClickListener(navigateToDetailsView)
        binding.buttonDetails.setOnClickListener(navigateToDetailsView)

        binding.buttonReceive.setOnClickListener {
            NavHostFragment.findNavController(itemView.findFragment())
                .navigateSafe(
                    WalletFragmentDirections.actionNavigationWalletToReceiveToWalletFragment(
                        wallet.walletConfig.id
                    )
                )
        }

        binding.buttonSend.setOnClickListener {
            NavHostFragment.findNavController(itemView.findFragment())
                .navigateSafe(
                    WalletFragmentDirections.actionNavigationWalletToSendFundsFragment(
                        wallet.walletConfig.id
                    )
                )
        }

        binding.walletSettings.setOnClickListener {
            NavHostFragment.findNavController(itemView.findFragment())
                .navigateSafe(
                    WalletFragmentDirections.actionNavigationWalletToWalletConfigFragment(
                        wallet.walletConfig.id
                    )
                )
        }

        val launchUnfoldTokenFieldChange: (v: View) -> Unit = {
            GlobalScope.launch {
                AppDatabase.getInstance(it.context).walletDao().updateWalletTokensUnfold(
                    wallet.walletConfig.id,
                    !wallet.walletConfig.unfoldTokens
                )
                // we don't need to update UI here - the DB change will trigger a rebind of the card
            }
        }
        binding.walletTokenUnfold.setOnClickListener(launchUnfoldTokenFieldChange)
        binding.labelTokenNum.setOnClickListener(launchUnfoldTokenFieldChange)
        binding.walletTokenNum.setOnClickListener(launchUnfoldTokenFieldChange)

        // Fill fiat value
        val nodeConnector = NodeConnector.getInstance()
        val ergoPrice = nodeConnector.fiatValue.value
        if (ergoPrice == 0f) {
            binding.walletFiat.visibility = View.GONE
        } else {
            binding.walletFiat.visibility = View.VISIBLE
            binding.walletFiat.amount = ergoPrice * binding.walletBalance.amount
            binding.walletFiat.setSymbol(nodeConnector.fiatCurrency.toUpperCase())
        }

        // Fill token entries
        binding.walletTokenEntries.apply {
            removeAllViews()

            if (wallet.walletConfig.unfoldTokens) {
                val maxTokensToShow = 5
                val dontShowAll = tokens.size > maxTokensToShow
                val tokensToShow =
                    (if (dontShowAll) tokens.subList(
                        0,
                        maxTokensToShow - 1
                    ) else tokens)
                val layoutInflater = LayoutInflater.from(context)
                tokensToShow.forEach {
                    inflateAndBindTokenView(it, this, layoutInflater)
                }

                // in case we don't show all items, add a hint that not all items were shown
                if (dontShowAll) {
                    val itemBinding =
                        EntryWalletTokenBinding.inflate(
                            LayoutInflater.from(itemView.context),
                            this,
                            true
                        )

                    itemBinding.labelTokenName.setText(R.string.label_more_tokens)
                    itemBinding.labelTokenVal.text =
                        "+" + (tokens.size - maxTokensToShow + 1).toString()
                }
            }
        }
    }

}

class WalletDiffCallback(val oldList: List<Wallet>, val newList: List<Wallet>) :
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