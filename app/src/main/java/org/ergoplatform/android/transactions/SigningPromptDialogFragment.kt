package org.ergoplatform.android.transactions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentPromptSigningDialogBinding
import org.ergoplatform.android.ui.setQrCodeToImageView

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
                val qrPages = coldSigninRequestToQrChunks(it, QR_SIZE_LIMIT)
                // TODO handle data over 4K length
                setQrCodeToImageView(binding.qrCode, qrPages.first(), 400, 400)
            }
        })
        binding.buttonScanSignedTx.setOnClickListener {
            IntentIntegrator.forSupportFragment(this).initiateScan(setOf(IntentIntegrator.QR_CODE))
        }
    }

    private fun getViewModel() = ViewModelProvider(parentFragment as ViewModelStoreOwner)
        .get(SendFundsViewModel::class.java)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            result.contents?.let {
                if (isColdSignedTxChunk(it)) {
                    if (getQrChunkPagesCount(it) > 1) {
                        // TODO handle paged QR codes
                        Snackbar.make(
                            requireView(),
                            R.string.error_qr_pages_num,
                            Snackbar.LENGTH_LONG
                        ).setAnchorView(R.id.nav_view).show()
                    } else {
                        getViewModel().sendColdWalletSignedTx(listOf(it), requireContext())
                        dismiss()
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}