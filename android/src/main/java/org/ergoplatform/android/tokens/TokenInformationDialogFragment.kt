package org.ergoplatform.android.tokens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.databinding.FragmentTokenInformationBinding
import org.ergoplatform.android.ui.AndroidStringProvider
import org.ergoplatform.android.ui.copyStringToClipboard
import org.ergoplatform.android.ui.openUrlWithBrowser
import org.ergoplatform.getExplorerTokenUrl
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.tokens.getHttpContentLink

class TokenInformationDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentTokenInformationBinding? = null
    private val binding get() = _binding!!

    private val args: TokenInformationDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTokenInformationBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonDone.setOnClickListener { buttonDone() }

        val viewModel: TokenInformationViewModel by viewModels()

        viewModel.init(args.tokenId, requireContext())

        viewModel.tokenInfo.observe(viewLifecycleOwner) { token ->
            binding.progressCircular.visibility = View.GONE
            token?.apply {
                binding.mainLayout.visibility = View.VISIBLE
                updateLayout(viewModel)
                binding.labelMintingTxId.setOnClickListener {
                    openUrlWithBrowser(requireContext(), getExplorerTxUrl(mintingTxId))
                }
            } ?: run { binding.tvError.visibility = View.VISIBLE }
        }

        viewModel.downloadState.observe(viewLifecycleOwner) {
            updateLayout(viewModel, true)
        }

        binding.labelTokenId.setOnClickListener {
            openUrlWithBrowser(
                requireContext(),
                getExplorerTokenUrl(binding.labelTokenId.text.toString())
            )
        }
        binding.labelTokenDescription.setOnClickListener {
            // XML animateLayoutChanges does not work in BottomSheet
            TransitionManager.beginDelayedTransition(binding.mainLayout)
            binding.labelTokenDescription.maxLines =
                if (binding.labelTokenDescription.maxLines == 5) 1000 else 5
        }
        binding.buttonDownloadContent.setOnClickListener {
            viewModel.uiLogic.downloadContent(
                Preferences(requireContext())
            )
        }
        binding.labelContentLink.setOnClickListener {
            viewModel.uiLogic.eip4Token?.nftContentLink?.let { contentLink ->
                val context = requireContext()
                val success = openUrlWithBrowser(context, contentLink)

                if (!success) {
                    viewModel.uiLogic.eip4Token!!.getHttpContentLink(Preferences(requireContext()))
                        ?.let { openUrlWithBrowser(context, it) }
                }
            }
        }
        binding.labelContentHash.setOnClickListener {
            copyStringToClipboard(binding.labelContentHash.text.toString(), requireContext(), null)
        }
    }

    private fun updateLayout(
        viewModel: TokenInformationViewModel,
        onlyPreview: Boolean = false
    ) {
        val context = requireContext()
        val tokenInformationLayoutView = TokenInformationLayoutView(binding)
        if (onlyPreview) {
            tokenInformationLayoutView.updateNftPreview(viewModel.uiLogic)
        } else {
            tokenInformationLayoutView.updateLayout(
                viewModel.uiLogic,
                AndroidStringProvider(context),
                args.amount
            )
        }
    }

    private fun buttonDone() {
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}