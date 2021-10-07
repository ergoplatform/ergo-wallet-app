package org.ergoplatform.android.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentPromptSigningDialogQrPageBinding

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