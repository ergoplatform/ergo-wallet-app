package org.ergoplatform.android.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.CardPresentSigningResultBinding
import org.ergoplatform.android.databinding.CardScanMoreBinding
import org.ergoplatform.android.databinding.FragmentPromptSigningDialogQrPageBinding
import org.ergoplatform.transactions.QrCodePagesCollector

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

class QrPagerAdapter(val qrCodes: List<String>) : RecyclerView.Adapter<QrPageHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QrPageHolder {
        return QrPageHolder(
            FragmentPromptSigningDialogQrPageBinding.inflate(
                LayoutInflater.from(parent.context),
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

fun Fragment.refreshScanMoreCardInfo(
    binding: CardScanMoreBinding,
    qrPagesCollector: QrCodePagesCollector,
    errorMessage: String?
) {
    binding.labelScannedPages.text = getString(
        R.string.label_qr_pages_info,
        qrPagesCollector.pagesAdded.toString(),
        qrPagesCollector.pagesCount.toString()
    )
    binding.root.visibility = View.VISIBLE
    binding.labelErrorMessage.visibility =
        if (errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE
    binding.labelErrorMessage.text = errorMessage
}

fun Fragment.setupSigningResultCardBinding(
    binding: CardPresentSigningResultBinding,
    onSwitchRes: () -> Unit,
    lastPageDesc: Int = R.string.desc_show_signed,
    pageDesc: Int = R.string.desc_show_signed_multiple
) {
    binding.buttonScanNextQr.setOnClickListener {
        binding.qrCodePager.currentItem = binding.qrCodePager.currentItem + 1
    }

    binding.switchResolution.setOnClickListener {
        onSwitchRes()
    }

    binding.buttonDismiss.setOnClickListener {
        findNavController().popBackStack()
    }

    binding.qrCodePager.registerOnPageChangeCallback(object :
        ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            binding.refreshButtonState(lastPageDesc, pageDesc)
        }
    })
}

fun CardPresentSigningResultBinding.refreshButtonState(
    lastPageDesc: Int = R.string.desc_show_signed,
    pageDesc: Int = R.string.desc_show_signed_multiple
) {
    val lastPage = qrCodePager.currentItem + 1 == qrCodePager.adapter!!.itemCount
    buttonDismiss.visibility = if (lastPage) View.VISIBLE else View.GONE
    buttonScanNextQr.visibility = if (!lastPage) View.VISIBLE else View.GONE
    tvScanSignedDesc.setText(
        if (lastPage) lastPageDesc else pageDesc
    )
}