package org.ergoplatform.android.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import org.ergoplatform.android.AppDatabase
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentReceiveToWalletBinding
import org.ergoplatform.android.getExplorerPaymentRequestAddress
import org.ergoplatform.android.setQrCodeToImageView


/**
 * Shows information and QR for receiving funds to a wallet
 */
class ReceiveToWalletFragment : Fragment() {

    private var _binding: FragmentReceiveToWalletBinding? = null
    private val binding get() = _binding!!

    private val args: ReceiveToWalletFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentReceiveToWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val wallet =
                AppDatabase.getInstance(requireContext()).walletDao().loadWalletById(args.walletId)

            wallet?.let {
                binding.publicAddress.text = wallet.publicAddress
                binding.walletName.text = wallet.displayName

                refreshQrCode()

                binding.buttonCopy.setOnClickListener {
                    val clipboard = getSystemService(
                        requireContext(),
                        ClipboardManager::class.java
                    )
                    val clip = ClipData.newPlainText("", wallet.publicAddress)
                    clipboard?.setPrimaryClip(clip)

                    Toast.makeText(requireContext(), R.string.label_copied, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun refreshQrCode() {
        binding.walletName.text?.let {
            setQrCodeToImageView(
                binding.qrCode,
                getExplorerPaymentRequestAddress(it.toString(), 2.5f, "hallo test"),
                400,
                400
            )
        }
    }
}