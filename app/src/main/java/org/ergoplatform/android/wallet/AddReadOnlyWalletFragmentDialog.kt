package org.ergoplatform.android.wallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import com.google.zxing.integration.android.IntentIntegrator
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
import org.ergoplatform.parsePaymentRequestFromQrCode

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

        binding.tvWalletAddress.setEndIconOnClickListener {
            IntentIntegrator.forSupportFragment(this).initiateScan(setOf(IntentIntegrator.QR_CODE))
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

            val context = requireContext()
            GlobalScope.launch(Dispatchers.IO) {
                // make sure not to use dialog context within this block
                val walletDao = AppDatabase.getInstance(context).walletDao()
                val existingWallet = walletDao.loadWalletByFirstAddress(walletAddress)
                if (existingWallet == null) {
                    walletDao.insertAll(walletConfig)
                    NodeConnector.getInstance().invalidateCache()
                }
            }
            NavHostFragment.findNavController(requireParentFragment())
                .navigateSafe(AddReadOnlyWalletFragmentDialogDirections.actionAddReadOnlyWalletFragmentDialogToWalletList())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                val content = parsePaymentRequestFromQrCode(it)
                content?.let { binding.tvWalletAddress.editText?.setText(content.address) }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}