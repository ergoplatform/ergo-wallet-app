package org.ergoplatform.android.tokens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.TokenAmount
import org.ergoplatform.android.R
import org.ergoplatform.android.databinding.FragmentTokenInformationBinding
import org.ergoplatform.android.ui.openUrlWithBrowser
import org.ergoplatform.getExplorerTokenUrl
import org.ergoplatform.getExplorerTxUrl
import org.ergoplatform.tokens.isSingularToken

// TODO https://developer.android.com/guide/topics/media/media-formats
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
                binding.labelTokenName.text =
                    if (displayName.isBlank()) getString(R.string.label_unnamed_token) else displayName
                binding.labelTokenId.text = tokenId
                binding.labelTokenDescription.text =
                    if (description.isNotBlank()) description else getString(R.string.label_no_description)
                binding.labelSupplyAmount.text =
                    TokenAmount(fullSupply, decimals).toStringUsFormatted(false)
                binding.labelBalanceAmount.text =
                    TokenAmount(args.amount, decimals).toStringUsFormatted(false)

                val showBalance = args.amount > 0 && !isSingularToken()
                binding.labelBalanceAmount.visibility = if (showBalance) View.VISIBLE else View.GONE
                binding.titleBalanceAmount.visibility = binding.labelBalanceAmount.visibility
                binding.labelSupplyAmount.visibility =
                    if (isSingularToken()) View.GONE else View.VISIBLE
                binding.titleSupplyAmount.visibility = binding.labelSupplyAmount.visibility

                binding.labelMintingTxId.setOnClickListener {
                    openUrlWithBrowser(requireContext(), getExplorerTxUrl(mintingTxId))
                }

            } ?: run { binding.tvError.visibility = View.VISIBLE }
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
    }

    private fun buttonDone() {
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}