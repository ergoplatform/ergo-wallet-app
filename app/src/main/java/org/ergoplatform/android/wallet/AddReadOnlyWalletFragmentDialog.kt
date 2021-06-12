package org.ergoplatform.android.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.NodeConnector
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentAddReadOnlyWalletDialogBinding
import org.ergoplatform.android.isValidErgoAddress
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.navigateSafe

/**
 * Add a wallet read-only by address
 */
class AddReadOnlyWalletFragmentDialog : FullScreenFragmentDialog() {

    private var _binding: FragmentAddReadOnlyWalletDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAddReadOnlyWalletDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonAddWallet.setOnClickListener {
            val walletAddress = binding.tvWalletAddress.editText?.text?.toString()

            walletAddress?.let {
                addWalletToDb(walletAddress)
            }
        }
    }

    private fun addWalletToDb(walletAddress: String) {
        if (!isValidErgoAddress(walletAddress)) {
            binding.tvWalletAddress.error = getString(R.string.error_receiver_address)
        } else {
            val walletConfig =
                WalletConfigDbEntity(
                    0,
                    getString(R.string.label_wallet_default),
                    walletAddress,
                    0,
                    null
                )

            GlobalScope.launch(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext()).walletDao().insertAll(walletConfig)
                NodeConnector.getInstance().invalidateCache()
            }
            NavHostFragment.findNavController(requireParentFragment())
                .navigateSafe(AddReadOnlyWalletFragmentDialogDirections.actionAddReadOnlyWalletFragmentDialogToWalletList())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}