package org.ergoplatform.android.wallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.RoomWalletDbProvider
import org.ergoplatform.android.databinding.FragmentAddReadOnlyWalletDialogBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.FullScreenFragmentDialog
import org.ergoplatform.android.ui.navigateSafe
import org.ergoplatform.uilogic.wallet.AddReadOnlyWalletUiLogic

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
                val context = requireContext()
                viewLifecycleOwner.lifecycleScope.launch {
                    val success = AndroidAddReadOnlyWalletUiLogic().addWalletToDb(
                        walletAddress,
                        RoomWalletDbProvider(AppDatabase.getInstance(context)),
                        AndroidStringProvider(context),
                        binding.inputWalletName.editText?.text?.toString()
                    )
                    if (success) {
                        NavHostFragment.findNavController(requireParentFragment())
                            .navigateSafe(AddReadOnlyWalletFragmentDialogDirections.actionAddReadOnlyWalletFragmentDialogToWalletList())
                    }
                }
            }
        }

        binding.tvWalletAddress.setEndIconOnClickListener {
            IntentIntegrator.forSupportFragment(this).initiateScan(setOf(IntentIntegrator.QR_CODE))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                binding.tvWalletAddress.editText?.setText(
                    AndroidAddReadOnlyWalletUiLogic().getInputFromQrCode(it)
                )
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class AndroidAddReadOnlyWalletUiLogic :
        AddReadOnlyWalletUiLogic() {
        override fun setErrorMessage(message: String) {
            binding.tvWalletAddress.error = message
        }

    }
}