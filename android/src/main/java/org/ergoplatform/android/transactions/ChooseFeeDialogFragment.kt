package org.ergoplatform.android.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.ergoplatform.ApiServiceManager
import org.ergoplatform.android.Preferences
import org.ergoplatform.android.databinding.FragmentChooseFeeDialogBinding
import org.ergoplatform.android.databinding.FragmentChooseFeeDialogItemBinding
import org.ergoplatform.android.ui.AndroidStringProvider

/**
 * Let the user choose or edit the fee amount
 */
class ChooseFeeDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentChooseFeeDialogBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: SendFundsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChooseFeeDialogBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(parentFragment as ViewModelStoreOwner)
            .get(SendFundsViewModel::class.java)
        viewModel.uiLogic.fetchSuggestedFeeData(
            ApiServiceManager.getOrInit(
                Preferences(
                    requireContext()
                )
            )
        )
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.inputFeeAmount.setText(viewModel.uiLogic.feeAmount.toStringTrimTrailingZeros())

        viewModel.suggestedFees.observe(viewLifecycleOwner) {
            binding.layoutFeeItems.apply {
                removeAllViews()
                it.forEach { suggestedFee ->
                    val itemBinding =
                        FragmentChooseFeeDialogItemBinding.inflate(layoutInflater, this, true)

                    val stringProvider = AndroidStringProvider(requireContext())
                    itemBinding.labelFeeExecutionSpeed.text =
                        suggestedFee.getExecutionSpeedText(stringProvider)
                    itemBinding.labelFeeAmount.text =
                        suggestedFee.getFeeAmountText(stringProvider)
                    itemBinding.labelFeeExecutionTime.text = suggestedFee.getFeeExecutionTimeText(stringProvider)
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}