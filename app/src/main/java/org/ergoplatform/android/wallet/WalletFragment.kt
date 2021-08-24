package org.ergoplatform.android.wallet

import StageConstants
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.*
import org.ergoplatform.android.databinding.CardWalletBinding
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.android.databinding.FragmentWalletBinding
import org.ergoplatform.android.ui.formatTokenAmounts
import org.ergoplatform.android.ui.navigateSafe
import java.util.*


class WalletFragment : Fragment() {

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
                    walletAdapter.walletList = walletList.sortedBy {
                        it.walletConfig.displayName?.toLowerCase(
                            Locale.getDefault()
                        )
                    }

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
                if (nodeConnector.isRefreshing.value == false) {
                    binding.ergoLogoBack.clearAnimation()
                }
            }

        })
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (!nodeConnector.refreshByUser(requireContext())) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        nodeConnector.isRefreshing.observe(viewLifecycleOwner, { isRefreshing ->
            if (!isRefreshing) {
                binding.swipeRefreshLayout.isRefreshing = false
                binding.connectionError.visibility =
                    if (nodeConnector.lastHadError) View.VISIBLE else View.INVISIBLE
                refreshTimeSinceSyncLabel()
            } else {
                binding.ergoLogoBack.clearAnimation()
                binding.ergoLogoBack.startAnimation(rotateAnimation)
            }
        })
        nodeConnector.fiatValue.observe(viewLifecycleOwner, { value ->
            if (value == 0f) {
                binding.ergoPrice.visibility = View.GONE
            } else {
                binding.ergoPrice.visibility = View.VISIBLE
                binding.ergoPrice.amount = value.toDouble()
                binding.ergoPrice.setSymbol(nodeConnector.fiatCurrency.toUpperCase(Locale.getDefault()))
            }
            binding.labelErgoPrice.visibility = binding.ergoPrice.visibility
        })
    }

    private fun refreshTimeSinceSyncLabel() {
        val nodeConnector = NodeConnector.getInstance()
        val lastRefresMs = nodeConnector.lastRefreshMs
        binding.synctime.text = if (lastRefresMs > 0) getString(
            R.string.label_last_sync,
            if (System.currentTimeMillis() - lastRefresMs < 60000L) getString(R.string.label_last_sync_just_now) else
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
        NodeConnector.getInstance().refreshWhenNeeded(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        binding.walletBalance.amount = nanoErgsToErgs(wallet.state.map { it.balance ?: 0 }.sum())

        // Fill token headline
        val tokenCount = wallet.tokens.size
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
        val unconfirmed = (wallet.state.map { it.unconfirmedBalance ?: 0 }.sum())
        binding.walletUnconfirmed.amount = nanoErgsToErgs(unconfirmed)
        binding.walletUnconfirmed.visibility = if (unconfirmed == 0L) View.GONE else View.VISIBLE
        binding.labelWalletUnconfirmed.visibility = binding.walletUnconfirmed.visibility

        // Set button listeners
        binding.buttonViewTransactions.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(StageConstants.EXPLORER_WEB_ADDRESS + "en/addresses/" + wallet.walletConfig.firstAddress)
            )
            binding.root.context.startActivity(browserIntent)
        }

        binding.buttonReceive.setOnClickListener {
            NavHostFragment.findNavController(itemView.findFragment())
                .navigateSafe(
                    WalletFragmentDirections.actionNavigationWalletToReceiveToWalletFragment(
                        wallet.walletConfig.id
                    )
                )
        }

        binding.buttonSend.isEnabled = wallet.walletConfig.secretStorage != null
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
        binding.walletTokenEntries.setOnClickListener(launchUnfoldTokenFieldChange)

        // Fill fiat value
        val nodeConnector = NodeConnector.getInstance()
        val ergoPrice = nodeConnector.fiatValue.value ?: 0f
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
                val dontShowAll = wallet.tokens.size > maxTokensToShow
                val tokensToShow =
                    (if (dontShowAll) wallet.tokens.subList(
                        0,
                        maxTokensToShow - 1
                    ) else wallet.tokens)
                tokensToShow.forEach {
                    val itemBinding =
                        EntryWalletTokenBinding.inflate(
                            LayoutInflater.from(itemView.context),
                            this,
                            true
                        )

                    itemBinding.labelTokenName.text = it.name
                    itemBinding.labelTokenVal.text =
                        formatTokenAmounts(it.amount ?: 0, it.decimals ?: 0, true)
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
                        "+" + (wallet.tokens.size - maxTokensToShow + 1).toString()
                }
            }
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