package org.ergoplatform.android.wallet.addresses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.android.databinding.FragmentWalletAddressDetailsDialogBinding
import org.ergoplatform.getAddressDerivationPath
import org.ergoplatform.android.ui.copyStringToClipboard
import org.ergoplatform.uilogic.wallet.addresses.WalletAddressDialogUiLogic

/**
 * Wallet address detail bottom sheet to edit an address label or delete the address
 */
class WalletAddressDetailsDialog : BottomSheetDialogFragment() {
    private var _binding: FragmentWalletAddressDetailsDialogBinding? = null
    private val binding get() = _binding!!

    private val args: WalletAddressDetailsDialogArgs by navArgs()

    private val uiLogic = WalletAddressDialogUiLogic()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentWalletAddressDetailsDialogBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.descriptiveLabel.editText?.setOnEditorActionListener { _, _, _ ->
            binding.buttonApply.callOnClick()
            true
        }

        val addrId = args.walletAddressId.toLong()

        binding.buttonApply.setOnClickListener { saveLabel(addrId) }
        binding.buttonRemove.setOnClickListener { deleteAddress(addrId) }

        viewLifecycleOwner.lifecycleScope.launch {
            val walletAddress =
                AppDatabase.getInstance(requireContext()).walletDao().loadWalletAddress(addrId.toInt())

            binding.publicAddress.text = walletAddress?.publicAddress
            binding.descriptiveLabel.editText?.setText(walletAddress?.label)
            binding.publicAddress.setOnClickListener {
                copyStringToClipboard(
                    walletAddress!!.publicAddress,
                    requireContext(), null
                )
            }
            binding.derivationPath.text =
                getAddressDerivationPath(walletAddress?.derivationIndex ?: 0)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun deleteAddress(addrId: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            uiLogic.deleteWalletAddress(RoomWalletDbProvider(AppDatabase.getInstance(requireContext())), addrId)
        }
        dismiss()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveLabel(addrId: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            val label = binding.descriptiveLabel.editText?.text?.toString()
            uiLogic.saveWalletAddressLabel(
                RoomWalletDbProvider(AppDatabase.getInstance(requireContext())),
                addrId,
                label
            )
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}