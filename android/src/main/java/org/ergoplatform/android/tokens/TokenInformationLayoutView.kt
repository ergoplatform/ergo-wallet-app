package org.ergoplatform.android.tokens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentTokenInformationBinding
import org.ergoplatform.uilogic.tokens.TokenInformationLayoutLogic
import org.ergoplatform.uilogic.tokens.TokenInformationModelLogic
import kotlin.math.max


class TokenInformationLayoutView(private val binding: FragmentTokenInformationBinding) :
    TokenInformationLayoutLogic() {

    override fun setTokenTextFields(displayName: String, tokenId: String, description: String) {
        binding.labelTokenName.text = displayName
        binding.labelTokenId.text = tokenId
        binding.labelTokenDescription.text = description
    }

    override fun setLabelSupplyAmountText(supplyAmount: String?) {
        binding.labelSupplyAmount.visibility =
            if (supplyAmount == null) View.GONE else View.VISIBLE
        binding.labelSupplyAmount.text = supplyAmount
        binding.titleSupplyAmount.visibility = binding.labelSupplyAmount.visibility
    }

    override fun setBalanceAmountAndValue(amount: String?, balanceValue: String?) {
        binding.labelBalanceAmount.text = amount
        binding.labelBalanceAmount.visibility = if (amount != null) View.VISIBLE else View.GONE
        binding.titleBalanceAmount.visibility = binding.labelBalanceAmount.visibility
        binding.labelBalanceValue.visibility =
            if (balanceValue != null) View.VISIBLE else View.GONE
        binding.labelBalanceValue.text = balanceValue

    }

    override fun setTokenGenuine(genuineFlag: Int) {
        binding.labelTokenName.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0, 0, getGenuineDrawableId(genuineFlag), 0
        )
    }

    override fun setNftLayoutVisibility(visible: Boolean) {
        binding.layoutNft.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setContentLinkText(linkText: String) {
        binding.labelContentLink.text = linkText
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

    override fun showNftPreview(
        downloadState: TokenInformationModelLogic.StateDownload,
        downloadPercent: Float,
        content: ByteArray?
    ) {
        binding.layoutPreview.visibility = when (downloadState) {
            TokenInformationModelLogic.StateDownload.NOT_AVAILABLE -> View.GONE
            TokenInformationModelLogic.StateDownload.NOT_STARTED -> View.GONE
            TokenInformationModelLogic.StateDownload.RUNNING -> View.VISIBLE
            TokenInformationModelLogic.StateDownload.DONE -> View.VISIBLE
            TokenInformationModelLogic.StateDownload.ERROR -> View.VISIBLE
        }

        binding.descDownloadContent.visibility =
            if (downloadState == TokenInformationModelLogic.StateDownload.NOT_STARTED) View.VISIBLE else View.GONE
        binding.buttonDownloadContent.visibility = binding.descDownloadContent.visibility
        binding.previewProgress.visibility =
            if (downloadState == TokenInformationModelLogic.StateDownload.RUNNING) View.VISIBLE else View.GONE

        val hasError = content?.let {
            try {
                val image: Drawable =
                    BitmapDrawable(
                        binding.root.resources,
                        decodeSampledBitmapFromByteArray(it)
                    )
                binding.imgPreview.setImageDrawable(image)
                false
            } catch (t: Throwable) {
                true
            }
        } ?: false

        if (hasError || downloadState == TokenInformationModelLogic.StateDownload.ERROR) {
            binding.imgPreview.setImageResource(R.drawable.ic_warning_amber_24)
        }

    }

    private fun decodeSampledBitmapFromByteArray(
        bitmapBytes: ByteArray
    ): Bitmap {
        // see https://developer.android.com/topic/performance/graphics/load-bitmap.html#read-bitmap

        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size, this)

            // Crashes when displaying images that are too big - calculate inSampleSize
            val minPixelSize = (300 * binding.root.resources.displayMetrics.density).toInt()
            inSampleSize = calculateInSampleSize(
                this,
                max(binding.layoutNft.width, minPixelSize),
                minPixelSize
            )

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false

            BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size, this)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    override fun showNftHashValidation(hashValid: Boolean?) {
        binding.labelContentHash.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0, 0, when (hashValid) {
                true -> R.drawable.ic_verified_18
                false -> R.drawable.ic_suspicious_18
                null -> 0
            }, 0
        )
    }
}