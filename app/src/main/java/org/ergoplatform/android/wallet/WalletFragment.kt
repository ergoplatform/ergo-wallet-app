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
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.NodeConnector
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.CardWalletBinding
import org.ergoplatform.android.databinding.FragmentWalletBinding
import org.ergoplatform.android.nanoErgsToErgs
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
                binding.ergoPrice.amount = value
                binding.ergoPrice.setSymbol(nodeConnector.fiatCurrency.toUpperCase(Locale.getDefault()))
            }
            binding.labelErgoPrice.visibility = binding.ergoPrice.visibility
        })
    }

    private fun refreshTimeSinceSyncLabel() {
        val nodeConnector = NodeConnector.getInstance()
        val lastRefresMs = nodeConnector.lastRefresMs
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
        binding.walletBalance.amount = nanoErgsToErgs(wallet.state?.balance ?: 0)
        binding.walletTransactions.text = (wallet.state?.transactions ?: 0).toString()

        val unconfirmed = (wallet.state?.unconfirmedBalance ?: 0) - (wallet.state?.balance ?: 0)
        binding.walletUnconfirmed.amount = nanoErgsToErgs(unconfirmed)
        binding.walletUnconfirmed.visibility = if (unconfirmed == 0L) View.GONE else View.VISIBLE
        binding.labelWalletUnconfirmed.visibility = binding.walletUnconfirmed.visibility

        binding.buttonViewTransactions.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(StageConstants.EXPLORER_WEB_ADDRESS + "en/addresses/" + wallet.walletConfig.publicAddress)
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

        val nodeConnector = NodeConnector.getInstance()
        val ergoPrice = nodeConnector.fiatValue.value ?: 0f
        if (ergoPrice == 0f) {
            binding.walletFiat.visibility = View.GONE
        } else {
            binding.walletFiat.visibility = View.VISIBLE
            binding.walletFiat.amount = ergoPrice * binding.walletBalance.amount
            binding.walletFiat.setSymbol(nodeConnector.fiatCurrency.toUpperCase())
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