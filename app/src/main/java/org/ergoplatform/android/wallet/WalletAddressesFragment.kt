package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.CardWalletAddressBinding
import org.ergoplatform.android.databinding.FragmentWalletAddressesBinding
import org.ergoplatform.android.nanoErgsToErgs


/**
 * Manages wallet derived addresses
 */
class WalletAddressesFragment : Fragment() {

    var _binding: FragmentWalletAddressesBinding? = null
    val binding: FragmentWalletAddressesBinding get() = _binding!!

    private val args: WalletAddressesFragmentArgs by navArgs()
    private lateinit var viewModel: WalletAddressesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentWalletAddressesBinding.inflate(layoutInflater, container, false)

        viewModel = ViewModelProvider(this).get(WalletAddressesViewModel::class.java)
        viewModel.init(requireContext(), args.walletId)
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val walletAddressesAdapter = WalletAddressesAdapter()
        binding.recyclerview.adapter = walletAddressesAdapter

        viewModel.addresses.observe(viewLifecycleOwner, {
            binding.walletName.text = viewModel.wallet?.walletConfig?.displayName
            walletAddressesAdapter.wallet = viewModel.wallet
            walletAddressesAdapter.addressList = it
        })
    }

    class WalletAddressesAdapter : RecyclerView.Adapter<WalletAddressViewHolder>() {
        var wallet: WalletDbEntity? = null
        var addressList: List<WalletAddressDbEntity> = emptyList()
            set(value) {
                val diffCallback = WalletAddressDiffCallback(field, value)
                field = value
                val diffResult = DiffUtil.calculateDiff(diffCallback)
                diffResult.dispatchUpdatesTo(this)
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletAddressViewHolder {
            return WalletAddressViewHolder(
                CardWalletAddressBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: WalletAddressViewHolder, position: Int) {
            holder.bind(addressList.get(position), wallet)
        }

        override fun getItemCount(): Int {
            // TODO add an extra item for "add another address"
            return addressList.size
        }
    }

    class WalletAddressViewHolder(val binding: CardWalletAddressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dbEntity: WalletAddressDbEntity, wallet: WalletDbEntity?) {
            val ctx = binding.root.context
            val isDerivedAddress = dbEntity.derivationIndex > 0
            binding.addressIndex.visibility =
                if (isDerivedAddress) View.VISIBLE else View.GONE
            binding.addressIndex.text = dbEntity.derivationIndex.toString()
            binding.addressLabel.text = dbEntity.label
                ?: (if (isDerivedAddress) ctx.getString(
                    R.string.label_wallet_address_derived,
                    dbEntity.derivationIndex.toString()
                ) else ctx.getString(R.string.label_wallet_main_address))
            binding.publicAddress.text = dbEntity.publicAddress
            binding.buttonDelete.visibility =
                if (isDerivedAddress) View.VISIBLE else View.GONE

            val state = wallet?.getStateForAddress(dbEntity.publicAddress)
            val tokens = wallet?.getTokensForAddress(dbEntity.publicAddress)
            binding.addressBalance.amount = nanoErgsToErgs(state?.balance ?: 0)
            binding.labelTokenNum.visibility =
                if (tokens.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.labelTokenNum.text =
                ctx.getString(R.string.label_wallet_token_balance, (tokens?.size ?: 0).toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

class WalletAddressDiffCallback(
    val oldList: List<WalletAddressDbEntity>,
    val newList: List<WalletAddressDbEntity>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList.get(oldItemPosition).derivationIndex == newList.get(newItemPosition).derivationIndex
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // always redraw
        return false
    }
}
