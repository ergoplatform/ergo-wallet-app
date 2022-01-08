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
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentPromptSigningDialogBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.QrPagerAdapter
import org.ergoplatform.transactions.QR_SIZE_LIMIT
import org.ergoplatform.transactions.coldSigningRequestToQrChunks
import org.ergoplatform.transactions.getColdSignedTxChunk

class SigningPromptDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentPromptSigningDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPromptSigningDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = getViewModel()
        viewModel.signingPromptData.observe(viewLifecycleOwner, {
            it?.let {
                val qrPages = coldSigningRequestToQrChunks(it, QR_SIZE_LIMIT)
                binding.qrCodePager.adapter = QrPagerAdapter(qrPages)

                refreshButtonState()
            }
        })
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
    }

    private fun refreshButtonState() {
        val lastPage =
            binding.qrCodePager.currentItem + 1 == binding.qrCodePager.adapter!!.itemCount
        binding.buttonScanSignedTx.visibility = if (lastPage) View.VISIBLE else View.GONE
        binding.buttonScanNextQr.visibility = if (!lastPage) View.VISIBLE else View.GONE
        binding.tvDesc.setText(if (lastPage) R.string.desc_prompt_signing else R.string.desc_prompt_signing_multiple)
    }

    private fun getViewModel() = ViewModelProvider(parentFragment as ViewModelStoreOwner)
        .get(SendFundsViewModel::class.java)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let { qrCode ->
                val qrChunk = getColdSignedTxChunk(qrCode)
                qrChunk?.let {
                    if (it.pages > 1) {
                        // TODO handle paged QR codes
                        Snackbar.make(
                            requireView(),
                            R.string.error_qr_pages_num,
                            Snackbar.LENGTH_LONG
                        ).setAnchorView(R.id.nav_view).show()
                    } else {
                        val context = requireContext()
                        getViewModel().uiLogic.sendColdWalletSignedTx(
                            listOf(qrCode),
                            Preferences(context),
                            AndroidStringProvider(context)
                        )
                        dismiss()
                    }
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