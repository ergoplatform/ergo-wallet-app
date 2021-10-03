package org.ergoplatform.android.transactions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentPromptSigningDialogBinding
import org.ergoplatform.android.databinding.FragmentPromptSigningDialogQrPageBinding
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
        val lastPage = binding.qrCodePager.currentItem + 1 == binding.qrCodePager.adapter!!.itemCount
        binding.buttonScanSignedTx.visibility = if (lastPage) View.VISIBLE else View.GONE
        binding.buttonScanNextQr.visibility = if (!lastPage) View.VISIBLE else View.GONE
        binding.tvDesc.setText(if (lastPage) R.string.desc_prompt_signing else R.string.desc_prompt_signing_multiple)
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class QrPageHolder(val binding: FragmentPromptSigningDialogQrPageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(qrCode: String, position: Int, pagesNum: Int) {
            setQrCodeToImageView(binding.qrCode, qrCode, 400, 400)

            binding.qrPagesInfo.visibility = if (pagesNum > 1) View.VISIBLE else View.GONE
            binding.qrPagesInfo.text = binding.root.context.getString(
                R.string.label_qr_pages_info,
                (position + 1).toString(),
                pagesNum.toString()
            )
        }

    }

    inner class QrPagerAdapter(val qrCodes: List<String>) : RecyclerView.Adapter<QrPageHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QrPageHolder {
            return QrPageHolder(
                FragmentPromptSigningDialogQrPageBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: QrPageHolder, position: Int) {
            holder.bind(qrCodes[position], position, qrCodes.size)
        }

        override fun getItemCount(): Int {
            return qrCodes.size
        }

    }
}