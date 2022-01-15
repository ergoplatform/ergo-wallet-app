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
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentColdWalletSigningBinding
import org.ergoplatform.android.ui.*
import org.ergoplatform.transactions.QR_DATA_LENGTH_LIMIT
import org.ergoplatform.transactions.QR_DATA_LENGTH_LOW_RES
import org.ergoplatform.transactions.coldSigningResponseToQrChunks
import org.ergoplatform.transactions.reduceBoxes

/**
 * Scans cold wallet signing request qr codes, signs the transaction, presents a qr code to go back
 */
class ColdWalletSigningFragment : AbstractAuthenticationFragment() {

    var _binding: FragmentColdWalletSigningBinding? = null
    val binding get() = _binding!!

    private val args: ColdWalletSigningFragmentArgs by navArgs()

    private var scaleDown = false

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

        addQrCodeChunk(args.qrCode)
        viewModel.setWalletId(args.walletId, requireContext())

        viewModel.lockInterface.observe(viewLifecycleOwner, {
            if (it)
                ProgressBottomSheetDialogFragment.showProgressDialog(childFragmentManager)
            else
                ProgressBottomSheetDialogFragment.dismissProgressDialog(childFragmentManager)
        })

        viewModel.signingResult.observe(viewLifecycleOwner, {
            if (it?.success == true && viewModel.signedQrCode != null) {
                binding.transactionInfo.root.visibility = View.GONE
                binding.cardSigningResult.visibility = View.VISIBLE
                binding.cardScanMore.visibility = View.GONE

                binding.switchResolution.visibility =
                    if (viewModel.signedQrCode!!.length > QR_DATA_LENGTH_LOW_RES) View.VISIBLE else View.GONE
                setQrData()

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
        binding.transactionInfo.buttonSignTx.setOnClickListener {
            viewModel.wallet?.let {
                startAuthFlow(it.walletConfig)
            }
        }

        binding.buttonScanNextQr.setOnClickListener {
            binding.qrCodePager.currentItem = binding.qrCodePager.currentItem + 1
        }

        binding.switchResolution.setOnClickListener {
            scaleDown = !scaleDown
            setQrData()
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

    private fun setQrData() {
        viewModel.signedQrCode?.let {
            binding.qrCodePager.adapter = QrPagerAdapter(
                coldSigningResponseToQrChunks(
                    it,
                    if (scaleDown) QR_DATA_LENGTH_LOW_RES else QR_DATA_LENGTH_LIMIT
                )
            )
            refreshButtonState()
        }
    }

    private fun addQrCodeChunk(qrCode: String?) {
        qrCode?.let { viewModel.uiLogic.addQrCodeChunk(qrCode) }

        val transactionInfo = viewModel.uiLogic.transactionInfo

        // don't show transaction info when we already have a signing result
        if (viewModel.signedQrCode != null)
            return

        if (transactionInfo == null) {
            // refresh information on scanned codes
            binding.labelScannedPages.text = getString(
                R.string.label_qr_pages_info,
                viewModel.uiLogic.qrPagesCollector.pagesAdded.toString(),
                viewModel.uiLogic.qrPagesCollector.pagesCount.toString()
            )
            binding.cardScanMore.visibility = View.VISIBLE
            val errorMessage = viewModel.uiLogic.lastErrorMessage
            binding.labelErrorMessage.visibility =
                if (errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.labelErrorMessage.text = errorMessage
        }

        transactionInfo?.reduceBoxes()?.let {
            binding.transactionInfo.root.visibility = View.VISIBLE
            binding.cardScanMore.visibility = View.GONE

            binding.transactionInfo.bindTransactionInfo(it, layoutInflater)
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