package org.ergoplatform.android.wallet.addresses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.CardWalletAddressBinding
import org.ergoplatform.android.databinding.FragmentWalletAddressesBinding
import org.ergoplatform.android.ui.AbstractAuthenticationFragment
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.wallet.addresses.isDerivedAddress


/**
 * Manages wallet derived addresses
 */
class WalletAddressesFragment : AbstractAuthenticationFragment() {

    private var _binding: FragmentWalletAddressesBinding? = null
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

        viewModel.lockProgress.observe(viewLifecycleOwner, {
            walletAddressesAdapter.addAddrHolder?.setProgress(it)
        })

        viewModel.addresses.observe(viewLifecycleOwner, {
            binding.walletName.text = viewModel.wallet?.walletConfig?.displayName
            walletAddressesAdapter.wallet = viewModel.wallet
            walletAddressesAdapter.addressList = it
        })
    }

    override fun proceedAuthFlowFromBiometrics() {
        viewModel.addAddressWithBiometricAuth(requireContext())
    }

    override fun proceedAuthFlowWithPassword(password: String) =
        viewModel.addAddressWithPass(requireContext(), password)

    inner class WalletAddressesAdapter : RecyclerView.Adapter<WalletAddressViewHolder>() {
        // holder that holds the add address button, for showing the progress bar
        var addAddrHolder: WalletAddressViewHolder? = null

        var wallet: Wallet? = null
        var addressList: List<WalletAddress> = emptyList()
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
            if (position == addressList.size) {
                holder.bindAddAddress()
                addAddrHolder = holder
            } else {
                holder.bindAddressInfo(addressList[position], wallet!!)
            }
        }

        override fun getItemCount(): Int {
            // we always have main address, so when this is empty db has not loaded yet
            return if (addressList.isEmpty()) 0 else addressList.size + 1
        }
    }

    inner class WalletAddressViewHolder(val binding: CardWalletAddressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindAddressInfo(dbEntity: WalletAddress, wallet: Wallet) {
            val isDerivedAddress = dbEntity.isDerivedAddress()

            binding.layoutNewAddress.visibility = View.GONE
            binding.buttonMoreMenu.visibility = if (isDerivedAddress) View.VISIBLE else View.GONE

            binding.cardView.setOnClickListener {
                if (isDerivedAddress) {
                    findNavController().navigateSafe(
                        WalletAddressesFragmentDirections.actionWalletAddressesFragmentToWalletAddressDetailsDialog(
                            dbEntity.id.toInt()
                        )
                    )
                }
            }

            binding.addressInformation.apply {
                root.visibility = View.VISIBLE
                fillAddressInformation(dbEntity, wallet)
            }
        }

        fun bindAddAddress() {
            binding.buttonMoreMenu.visibility = View.GONE
            binding.layoutNewAddress.visibility = View.VISIBLE
            binding.addressInformation.root.visibility = View.GONE
            binding.cardView.setOnClickListener(null)

            val walletConfig = viewModel.wallet?.walletConfig
            binding.buttonAddAddress.setOnClickListener {
                viewModel.numAddressesToAdd = getNumAddressesToAdd()
                walletConfig?.let { walletConfig ->
                    walletConfig.secretStorage?.let {
                        startAuthFlow(walletConfig)
                    } ?: viewModel.addNextAddresses(requireContext(), null)
                }
            }
            binding.buttonAddAddress.isEnabled = viewModel.uiLogic.canDeriveAddresses()
            binding.sliderNumAddresses.progress = 0
            refreshAddButtonLabel()
            binding.sliderNumAddresses.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    refreshAddButtonLabel()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            })
        }

        fun setProgress(locked: Boolean) {
            binding.buttonAddAddress.visibility = if (locked) View.INVISIBLE else View.VISIBLE
            binding.progressBar.visibility = if (locked) View.VISIBLE else View.INVISIBLE
        }

        private fun refreshAddButtonLabel() {
            val numAddresses = getNumAddressesToAdd()
            binding.buttonAddAddress.text =
                if (numAddresses <= 1) getString(R.string.button_add_address) else
                    getString(R.string.button_add_addresses, numAddresses)
        }

        private fun getNumAddressesToAdd() = binding.sliderNumAddresses.progress + 1
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

class WalletAddressDiffCallback(
    val oldList: List<WalletAddress>,
    val newList: List<WalletAddress>
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
