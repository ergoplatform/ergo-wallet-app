package org.ergoplatform.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.android.databinding.FragmentShareQrBinding

/**
 * Shows a QR code and text to share
 */
class ShareWithQrDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentShareQrBinding? = null
    private val binding get() = _binding!!

    private val args: ShareWithQrDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentShareQrBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonDone.setOnClickListener { buttonDone() }

        val dataToShare = args.dateToShare

        binding.buttonCopy.setOnClickListener {
            copyStringToClipboard(dataToShare, requireContext(), null)
        }

        binding.textToShare.text = dataToShare

        setQrCodeToImageView(
            binding.qrCode,
            dataToShare,
            400,
            400
        )
    }

    private fun buttonDone() {
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}