package org.ergoplatform.android.mosaik

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentMosaikBinding
import org.ergoplatform.android.ui.copyStringToClipboard
import org.ergoplatform.android.ui.decodeSampledBitmapFromByteArray
import org.ergoplatform.android.ui.openUrlWithBrowser
import org.ergoplatform.mosaik.MosaikStyleConfig
import org.ergoplatform.mosaik.MosaikViewTree
import org.ergoplatform.mosaik.convertByteArrayToImageBitmap
import org.ergoplatform.mosaik.scrollMinAlpha

class MosaikFragment : Fragment() {
    private var _binding: FragmentMosaikBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MosaikViewModel by viewModels()
    private val args by navArgs<MosaikFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMosaikBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.browserEvent.observe(viewLifecycleOwner) { url ->
            url?.let { openUrlWithBrowser(requireContext(), url) }
        }
        viewModel.pasteToClipboardEvent.observe(viewLifecycleOwner) { text ->
            text?.let { copyStringToClipboard(text, requireContext(), binding.root) }
        }
        viewModel.showDialogEvent.observe(viewLifecycleOwner) { dialog ->
            dialog?.let {
                val builder = MaterialAlertDialogBuilder(requireContext())
                    .setMessage(dialog.message)
                    .setPositiveButton(dialog.positiveButtonText) { _, _ ->
                        dialog.positiveButtonClicked?.run()
                    }

                dialog.negativeButtonText?.let {
                    builder.setNegativeButton(dialog.negativeButtonText) { _, _ ->
                        dialog.negativeButtonClicked?.run()
                    }
                }

                builder.show()

            }
        }
        viewModel.manifestLiveData.observe(viewLifecycleOwner) { manifest ->
            binding.fragmentTitle.text = manifest?.appName

            // TODO Mosaik 0.1.1 enable navigate back
        }

        // set some custom vars for Compose environment
        val minPixelSize = (300 * binding.root.resources.displayMetrics.density).toInt()
        scrollMinAlpha = 0f
        convertByteArrayToImageBitmap = { byteArray ->
            decodeSampledBitmapFromByteArray(
                byteArray,
                minPixelSize,
                minPixelSize
            ).asImageBitmap()
        }
        binding.composeView.setContent {
            MosaikStyleConfig.apply {
                primaryLabelColor = colorResource(id = R.color.primary)
                secondaryLabelColor = colorResource(id = R.color.darkgrey)
                defaultLabelColor = colorResource(id = R.color.text_color)
                primaryButtonTextColor = colorResource(id = R.color.textcolor)
                secondaryButtonTextColor = colorResource(id = R.color.textcolor)
                secondaryButtonColor = colorResource(id = R.color.textcolor)
                secondaryButtonTextColor = colorResource(id = R.color.text_color_ondark)
                textButtonTextColor = colorResource(id = R.color.primary)
                textButtonColorDisabled = secondaryLabelColor
            }

            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    surface = colorResource(id = R.color.cardview_background),
                    isLight = resources.getBoolean(R.bool.isLight)
                )
            ) {
                MosaikViewTree(viewModel.mosaikRuntime.viewTree)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedHandler)

        viewModel.initialize(args.url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val backPressedHandler = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // TODO Mosaik 0.1.1 navigate back
        }

    }
}