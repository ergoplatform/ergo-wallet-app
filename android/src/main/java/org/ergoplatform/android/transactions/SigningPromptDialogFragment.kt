package org.ergoplatform.android.transactions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.zxing.integration.android.IntentIntegrator
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentPromptSigningDialogBinding
import org.ergoplatform.android.ui.QrPagerAdapter
import org.ergoplatform.android.ui.expandBottomSheetOnShow
import org.ergoplatform.transactions.QR_DATA_LENGTH_LIMIT
import org.ergoplatform.transactions.QR_DATA_LENGTH_LOW_RES
import org.ergoplatform.transactions.QrCodePagesCollector
import org.ergoplatform.transactions.coldSigningRequestToQrChunks


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
        val dataSource = getDataSource()
        dataSource.signingPromptData?.let {
            setQrCodePagerData(it)
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
            dataSource.signingPromptData?.let {
                scaleDown = !scaleDown
                setQrCodePagerData(it)
            }
        }

        binding.buttonScanSignedTx.setText(dataSource.lastPageButtonLabel)

        refreshScannedPagesInfo(dataSource.signingResponseQrCodePagesCollector)
    }

    private fun refreshScannedPagesInfo(pagesCollector: QrCodePagesCollector?) {
        binding.qrScannedPagesInfo.visibility =
            if (pagesCollector?.pagesAdded ?: 0 > 0) View.VISIBLE else View.INVISIBLE

        pagesCollector?.let {
            binding.qrScannedPagesInfo.text =
                getString(
                    R.string.label_qr_pages_info,
                    it.pagesAdded.toString(),
                    it.pagesCount.toString()
                )
        }
    }

    private fun setQrCodePagerData(data: String) {
        binding.switchResolution.visibility =
            if (data.length > QR_DATA_LENGTH_LOW_RES) View.VISIBLE else View.GONE
        val qrPages = getDataSource().signingRequestToQrChunks(
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
        val dataSource = getDataSource()
        binding.tvDesc.setText(if (lastPage) dataSource.lastPageDescriptionLabel else dataSource.descriptionLabel)
    }

    private fun getDataSource() = (parentFragment as SigningPromptDialogDataSource)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let { qrCode ->
                val dataSource = getDataSource()
                dataSource.signingResponseQrCodePagesCollector?.addPage(qrCode)
                if (dataSource.signingResponseQrCodePagesCollector?.hasAllPages() == true) {
                    dataSource.onSigningPromptResponseScanComplete()
                    dismiss()
                } else {
                    refreshScannedPagesInfo(dataSource.signingResponseQrCodePagesCollector)
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

interface SigningPromptDialogDataSource {
    val signingResponseQrCodePagesCollector: QrCodePagesCollector?
    fun onSigningPromptResponseScanComplete()
    val signingPromptData: String?
    fun signingRequestToQrChunks(serializedSigningRequest: String, sizeLimit: Int): List<String>
    val lastPageButtonLabel get() = R.string.button_scan_signed_tx
    val descriptionLabel get() = R.string.desc_prompt_cold_auth_multiple
    val lastPageDescriptionLabel get() = R.string.desc_prompt_cold_auth
}