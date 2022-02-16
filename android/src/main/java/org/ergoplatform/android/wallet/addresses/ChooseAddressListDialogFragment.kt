package org.ergoplatform.android.wallet.addresses

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.databinding.FragmentChooseAddressDialogBinding
import org.ergoplatform.android.databinding.FragmentChooseAddressDialogItemBinding
import org.ergoplatform.persistance.Wallet
import org.ergoplatform.persistance.WalletAddress
import org.ergoplatform.uilogic.wallet.addresses.ChooseAddressListAdapterLogic

/**
 * Let the user choose a derived address
 */
class ChooseAddressListDialogFragment : BottomSheetDialogFragment() {
    companion object {
        private const val ARG_WALLET_ID = "ARG_WALLET_ID"
        private const val ARG_SHOW_ALL_ADDRESSES = "ARG_SHOW_ALL"

        fun newInstance(
            walletId: Int,
            addShowAllEntry: Boolean = false
        ): ChooseAddressListDialogFragment {
            val addressChooser = ChooseAddressListDialogFragment()
            val args = Bundle()
            args.putInt(ARG_WALLET_ID, walletId)
            args.putBoolean(ARG_SHOW_ALL_ADDRESSES, addShowAllEntry)
            addressChooser.arguments = args
            return addressChooser
        }
    }

    private var _binding: FragmentChooseAddressDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChooseAddressDialogBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager =
            LinearLayoutManager(context)

        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.getInstance(context).walletDao()
                .loadWalletWithStateById(requireArguments().getInt(ARG_WALLET_ID))?.let {
                    binding.list.adapter = DisplayAddressesAdapter(
                        ChooseAddressListAdapterLogic(
                            it.toModel(),
                            requireArguments().getBoolean(ARG_SHOW_ALL_ADDRESSES)
                        )
                    )
                }
        }
    }

    private fun onChooseAddress(addressDerivationIdx: Int?) {
        (parentFragment as AddressChooserCallback).onAddressChosen(addressDerivationIdx)
        dismiss()
    }

    private inner class ViewHolder(val binding: FragmentChooseAddressDialogItemBinding) :
        RecyclerView.ViewHolder(binding.root), ChooseAddressListAdapterLogic.AddressHolder {

        override fun bindAddress(address: WalletAddress, wallet: Wallet) {
            binding.addressInformation.fillAddressInformation(address, wallet)
            binding.addressInformation.addressIndex.visibility = View.GONE
            binding.root.setOnClickListener {
                onChooseAddress(address.derivationIndex)
            }
        }

        override fun bindAllAddresses(wallet: Wallet) {
            binding.addressInformation.fillWalletAddressesInformation(wallet)
            binding.root.setOnClickListener {
                onChooseAddress(null)
            }
        }

    }

    private inner class DisplayAddressesAdapter(val adapterLogic: ChooseAddressListAdapterLogic) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            val binding = FragmentChooseAddressDialogItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
            binding.addressInformation.divider.visibility = View.GONE
            binding.addressInformation.publicAddress.visibility = View.GONE
            binding.addressInformation.addressLabel.gravity = Gravity.CENTER_HORIZONTAL
            return ViewHolder(binding)

        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            adapterLogic.bindViewHolder(holder, position)
        }

        override fun getItemCount(): Int {
            return adapterLogic.itemCount
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

interface AddressChooserCallback {
    fun onAddressChosen(addressDerivationIdx: Int?)
}