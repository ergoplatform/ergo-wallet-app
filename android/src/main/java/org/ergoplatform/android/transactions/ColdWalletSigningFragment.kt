package org.ergoplatform.android.transactions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import org.ergoplatform.ErgoAmount
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.EntryTransactionBoxBinding
import org.ergoplatform.android.databinding.EntryWalletTokenBinding
import org.ergoplatform.android.databinding.FragmentColdWalletSigningBinding
import org.ergoplatform.android.ui.*
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
            return ViewModelProvider(this).get(ColdWalletSigningViewModel::class.java)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = viewModel

        args.qrCode?.let { addQrCodeChunk(it) }
        viewModel.setWalletId(args.walletId, requireContext())

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

                binding.qrCodePager.adapter = QrPagerAdapter(viewModel.signedQrCode!!)
                refreshButtonState()

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
                            showDialogWithCopyOption(requireContext(), errorMsg)
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

        binding.buttonScanNextQr.setOnClickListener {
            binding.qrCodePager.currentItem = binding.qrCodePager.currentItem + 1
        }

        binding.buttonDismiss.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonScanMore.setOnClickListener {
            IntentIntegrator.forSupportFragment(this).initiateScan(setOf(IntentIntegrator.QR_CODE))
        }

        binding.qrCodePager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                refreshButtonState()
            }
        })
    }

    private fun addQrCodeChunk(qrCode: String) {
        val transactionInfo = viewModel.uiLogic.addQrCodeChunk(qrCode)

        // don't show transaction info when we already have a signing result
        if (viewModel.signedQrCode != null)
            return

        if (transactionInfo == null) {
            // refresh information on scanned codes
            binding.labelScannedPages.text = getString(
                R.string.label_qr_pages_info,
                viewModel.uiLogic.pagesAdded.toString(),
                viewModel.uiLogic.pagesQrCode.toString()
            )
            binding.cardScanMore.visibility = View.VISIBLE
            val errorMessage = viewModel.uiLogic.lastErrorMessage
            binding.labelErrorMessage.visibility = if (errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.labelErrorMessage.text = errorMessage
        }

        transactionInfo?.reduceBoxes()?.let {
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
    }

    private fun refreshButtonState() {
        val lastPage =
            binding.qrCodePager.currentItem + 1 == binding.qrCodePager.adapter!!.itemCount
        binding.buttonDismiss.visibility = if (lastPage) View.VISIBLE else View.GONE
        binding.buttonScanNextQr.visibility = if (!lastPage) View.VISIBLE else View.GONE
        binding.tvScanSignedDesc.setText(
            if (lastPage) R.string.desc_show_signed
            else R.string.desc_show_signed_multiple
        )
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
        return viewModel.signTxWithPassword(password, AndroidStringProvider(requireContext()))
    }

    override fun proceedAuthFlowFromBiometrics() {
        viewModel.signTxUserAuth(AndroidStringProvider(requireContext()))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                addQrCodeChunk(it)
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