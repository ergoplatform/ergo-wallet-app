package org.ergoplatform.android.transactions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.integration.android.IntentIntegrator
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentPromptSigningDialogBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.QrPagerAdapter
import org.ergoplatform.transactions.QR_DATA_LENGTH_LIMIT
import org.ergoplatform.transactions.QR_DATA_LENGTH_LOW_RES
import org.ergoplatform.transactions.QrCodePagesCollector
import org.ergoplatform.transactions.coldSigningRequestToQrChunks

import org.ergoplatform.android.ui.expandBottomSheetOnShow


/**
 * SigningPromptDialogFragment is shown when user makes a transaction on a read-only address, presenting QR code(s)
 * to scan with a cold wallet device.
 */
class SigningPromptDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentPromptSigningDialogBinding? = null
    private val binding get() = _binding!!

    private var scaleDown = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPromptSigningDialogBinding.inflate(inflater, container, false)
        expandBottomSheetOnShow()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = getViewModel()
        viewModel.signingPromptData.observe(viewLifecycleOwner) {
            it?.let {
                setQrCodePagerData(it)
            }
        }
        binding.qrCodePager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                refreshButtonState()
            }
        })
        binding.buttonScanSignedTx.setOnClickListener {
            IntentIntegrator.forSupportFragment(this).initiateScan(setOf(IntentIntegrator.QR_CODE))
        }
        binding.buttonScanNextQr.setOnClickListener {
            binding.qrCodePager.currentItem = binding.qrCodePager.currentItem + 1
        }
        binding.switchResolution.setOnClickListener {
            viewModel.signingPromptData.value?.let {
                scaleDown = !scaleDown
                setQrCodePagerData(it)
            }
        }

        refreshScannedPagesInfo(viewModel.uiLogic.signedTxQrCodePagesCollector)
    }

    private fun refreshScannedPagesInfo(pagesCollector: QrCodePagesCollector?) {
        binding.qrScannedPagesInfo.visibility =
            if (pagesCollector?.pagesAdded ?: 0 > 0) View.VISIBLE else View.INVISIBLE

        pagesCollector?.let {
            binding.qrScannedPagesInfo.text =
                getString(R.string.label_qr_pages_info, it.pagesAdded.toString(), it.pagesCount.toString())
        }
    }

    private fun setQrCodePagerData(data: String) {
        binding.switchResolution.visibility =
            if (data.length > QR_DATA_LENGTH_LOW_RES) View.VISIBLE else View.GONE
        val qrPages = coldSigningRequestToQrChunks(
            data,
            if (scaleDown) QR_DATA_LENGTH_LOW_RES else QR_DATA_LENGTH_LIMIT
        )
        binding.qrCodePager.adapter = QrPagerAdapter(qrPages)

        refreshButtonState()
    }

    private fun refreshButtonState() {
        val lastPage =
            binding.qrCodePager.currentItem + 1 == binding.qrCodePager.adapter!!.itemCount
        binding.buttonScanSignedTx.visibility = if (lastPage) View.VISIBLE else View.GONE
        binding.buttonScanNextQr.visibility = if (!lastPage) View.VISIBLE else View.GONE
        binding.tvDesc.setText(if (lastPage) R.string.desc_prompt_signing else R.string.desc_prompt_signing_multiple)
    }

    private fun getViewModel() = (parentFragment as SubmitTransactionFragment).viewModel

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let { qrCode ->
                val uiLogic = getViewModel().uiLogic
                uiLogic.signedTxQrCodePagesCollector?.addPage(qrCode)
                if (uiLogic.signedTxQrCodePagesCollector?.hasAllPages() == true) {
                    val context = requireContext()
                    uiLogic.sendColdWalletSignedTx(
                        Preferences(context),
                        AndroidStringProvider(context)
                    )
                    dismiss()
                } else {
                    refreshScannedPagesInfo(uiLogic.signedTxQrCodePagesCollector)
                }
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