package org.ergoplatform.android.tokens

import android.view.View
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentTokenInformationBinding
import org.ergoplatform.uilogic.tokens.TokenInformationNftLayoutUiLogic
import org.ergoplatform.uilogic.tokens.TokenInformationUiLogic
import android.graphics.BitmapFactory

import android.graphics.drawable.BitmapDrawable

import android.graphics.drawable.Drawable


class TokenInformationNftLayoutView(private val binding: FragmentTokenInformationBinding) :
    TokenInformationNftLayoutUiLogic() {
    override fun setNftLayoutVisibility(visible: Boolean) {
        binding.layoutNft.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setContentLinkText(linkText: String) {
        binding.labelContentLink.text = linkText
    }

    override fun setContentLinkClickListener(listener: Runnable?) {
        binding.labelContentLink.setOnClickListener(listener?.let { View.OnClickListener { listener.run() } })
    }

    override fun setContentHashText(hashText: String) {
        binding.labelContentHash.text = hashText
    }

    override fun setThumbnail(thumbnailType: Int) {
        val thumbnailDrawable = getThumbnailDrawableId(thumbnailType)
        binding.layoutThumbnail.visibility =
            if (thumbnailDrawable == 0) View.GONE else View.VISIBLE
        binding.imgThumbnail.setImageResource(thumbnailDrawable)
    }

    override fun showPreview(
        downloadState: TokenInformationUiLogic.StateDownload,
        downloadPercent: Float,
        content: ByteArray?
    ) {
        binding.layoutPreview.visibility = when (downloadState) {
            TokenInformationUiLogic.StateDownload.NOT_AVAILABLE -> View.GONE
            TokenInformationUiLogic.StateDownload.NOT_STARTED -> View.GONE
            TokenInformationUiLogic.StateDownload.RUNNING -> View.VISIBLE
            TokenInformationUiLogic.StateDownload.DONE -> View.VISIBLE
            TokenInformationUiLogic.StateDownload.ERROR -> View.VISIBLE
        }

        binding.descDownloadContent.visibility =
            if (downloadState == TokenInformationUiLogic.StateDownload.NOT_STARTED) View.VISIBLE else View.GONE
        binding.buttonDownloadContent.visibility = binding.descDownloadContent.visibility
        binding.previewProgress.visibility =
            if (downloadState == TokenInformationUiLogic.StateDownload.RUNNING) View.VISIBLE else View.GONE

        val hasError = content?.let {
            try {
                val image: Drawable =
                    BitmapDrawable(
                        binding.root.resources,
                        BitmapFactory.decodeByteArray(it, 0, it.size)
                    )
                binding.imgPreview.setImageDrawable(image)
                false
            } catch (t: Throwable) {
                true
            }
        } ?: false

        if (hasError || downloadState == TokenInformationUiLogic.StateDownload.ERROR) {
            binding.imgPreview.setImageResource(R.drawable.ic_warning_amber_24)
        }

    }

    override fun showHashValidation(hashValid: Boolean?) {
        binding.labelContentHash.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0, 0, when (hashValid) {
                true -> R.drawable.ic_verified_18
                false -> R.drawable.ic_suspicious_18
                null -> 0
            }, 0
        )
    }
}