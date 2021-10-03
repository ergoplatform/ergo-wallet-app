package org.ergoplatform.android.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import org.ergoplatform.ErgoAmount
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.EntryTransactionBoxBinding
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.android.databinding.FragmentColdWalletSigningBinding
import org.ergoplatform.android.ui.AbstractAuthenticationFragment
import org.ergoplatform.android.ui.ProgressBottomSheetDialogFragment
import org.ergoplatform.android.ui.setQrCodeToImageView
import org.ergoplatform.explorer.client.model.AssetInstanceInfo
import org.ergoplatform.transactions.reduceBoxes

/**
 * Scans cold wallet signing request qr codes, signs the transaction, presents a qr code to go back
 */
class ColdWalletSigningFragment : AbstractAuthenticationFragment() {

    var _binding: FragmentColdWalletSigningBinding? = null
    val binding get() = _binding!!

    private val args: ColdWalletSigningFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColdWalletSigningBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    private val viewModel: ColdWalletSigningViewModel
        get() {
            val viewModel = ViewModelProvider(this).get(ColdWalletSigningViewModel::class.java)
            return viewModel
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = viewModel

        args.qrCode?.let { viewModel.addQrCodeChunk(it) }
        viewModel.setWalletId(args.walletId, requireContext())

        viewModel.transactionInfo.observe(viewLifecycleOwner, {
            // don't show transaction info when we already have a signing result
            if (viewModel.signedQrCode != null)
                return@observe

            if (it == null) {
                // refresh information on scanned codes
                binding.labelScannedPages.text = getString(
                    R.string.label_qr_pages_info,
                    viewModel.pagesAdded.toString(),
                    viewModel.pagesQrCode.toString()
                )
                binding.cardScanMore.visibility = View.VISIBLE
            }

            it?.reduceBoxes()?.let {
                binding.transactionInfo.visibility = View.VISIBLE
                binding.cardScanMore.visibility = View.GONE

                binding.layoutInboxes.apply {
                    removeAllViews()

                    it.inputs.forEach { input ->
                        bindBoxView(this, input.value, input.address ?: input.boxId, input.assets)
                    }
                }

                binding.layoutOutboxes.apply {
                    removeAllViews()

                    it.outputs.forEach { output ->
                        bindBoxView(this, output.value, output.address, output.assets)
                    }
                }
            }
        })

        viewModel.lockInterface.observe(viewLifecycleOwner, {
            if (it)
                ProgressBottomSheetDialogFragment.showProgressDialog(childFragmentManager)
            else
                ProgressBottomSheetDialogFragment.dismissProgressDialog(childFragmentManager)
        })

        viewModel.signingResult.observe(viewLifecycleOwner, {
            if (it?.success == true && viewModel.signedQrCode != null) {
                binding.transactionInfo.visibility = View.GONE
                binding.cardSigningResult.visibility = View.VISIBLE
                binding.cardScanMore.visibility = View.GONE

                // TODO handle data over 4k length
                setQrCodeToImageView(binding.qrCode, viewModel.signedQrCode!!.first(), 400, 400)

            } else {
                binding.cardSigningResult.visibility = View.GONE

                it?.let {
                    val snackbar = Snackbar.make(
                        requireView(),
                        R.string.error_prepare_transaction,
                        Snackbar.LENGTH_LONG
                    )
                    it.errorMsg?.let { errorMsg ->
                        snackbar.setAction(
                            R.string.label_details
                        ) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setMessage(errorMsg)
                                .setPositiveButton(R.string.button_copy) { _, _ ->
                                    val clipboard = ContextCompat.getSystemService(
                                        requireContext(),
                                        ClipboardManager::class.java
                                    )
                                    val clip = ClipData.newPlainText("", errorMsg)
                                    clipboard?.setPrimaryClip(clip)
                                }
                                .setNegativeButton(R.string.label_dismiss, null)
                                .show()
                        }
                    }
                    snackbar.setAnchorView(R.id.nav_view).show()
                }
            }
        })

        // Button click listeners
        binding.buttonSignTx.setOnClickListener {
            viewModel.wallet?.let {
                startAuthFlow(it.walletConfig)
            }
        }

        binding.buttonDismiss.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonScanMore.setOnClickListener {
            IntentIntegrator.forSupportFragment(this).initiateScan(setOf(IntentIntegrator.QR_CODE))
        }
    }

    private fun bindBoxView(
        container: ViewGroup,
        value: Long?,
        address: String,
        assets: List<AssetInstanceInfo>?
    ) {
        val boxBinding = EntryTransactionBoxBinding.inflate(layoutInflater, container, true)
        boxBinding.boxErgAmount.text = getString(
            R.string.label_erg_amount,
            ErgoAmount(value ?: 0).toStringTrimTrailingZeros()
        )
        boxBinding.boxErgAmount.visibility =
            if (value == null || value == 0L) View.GONE else View.VISIBLE
        boxBinding.labelBoxAddress.text = address
        boxBinding.labelBoxAddress.setOnClickListener {
            boxBinding.labelBoxAddress.maxLines =
                if (boxBinding.labelBoxAddress.maxLines == 1) 10 else 1
        }

        boxBinding.boxTokenEntries.apply {
            removeAllViews()
            visibility = View.GONE

            assets?.forEach {
                visibility = View.VISIBLE
                val tokenBinding =
                    EntryWalletTokenBinding.inflate(layoutInflater, this, true)
                // we use the token id here, we don't have the name in the cold wallet context
                tokenBinding.labelTokenName.text = it.tokenId
                tokenBinding.labelTokenVal.text = it.amount.toString()
            }
        }
    }

    override fun proceedAuthFlowWithPassword(password: String): Boolean {
        return viewModel.signTxWithPassword(password)
    }

    override fun proceedAuthFlowFromBiometrics() {
        viewModel.signTxUserAuth()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                viewModel.addQrCodeChunk(it)
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