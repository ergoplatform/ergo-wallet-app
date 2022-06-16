package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.databinding.FragmentChooseWalletDialogBinding
import org.ergoplatform.persistance.WalletConfig

/**
 * Let the user choose a wallet. Similar to [ChooseSpendingWalletFragmentDialog], but bottom sheet
 * and used when user is not prompted without knowing what to do, but instead choose to
 * use another wallet
 */
class ChooseWalletListBottomSheetDialog : BottomSheetDialogFragment() {
    private var _binding: FragmentChooseWalletDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChooseWalletDialogBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        AppDatabase.getInstance(requireContext()).walletDao().getWalletsWithStates()
            .observe(viewLifecycleOwner) { walletList ->
                val wallets = walletList.map { it.toModel() }
                binding.listWallets.removeAllViews()

                addWalletChooserItemBindings(
                    layoutInflater,
                    binding.listWallets,
                    wallets,
                    true
                ) { walletConfig -> onChooseWallet(walletConfig) }
            }
    }

    private fun onChooseWallet(walletConfig: WalletConfig) {
        (parentFragment as WalletChooserCallback).onWalletChosen(walletConfig)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

interface WalletChooserCallback {
    fun onWalletChosen(walletConfig: WalletConfig)
}